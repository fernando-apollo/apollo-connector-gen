package com.apollographql.oas.select;

import com.apollographql.oas.converter.Main;
import com.apollographql.oas.select.context.RefCounter;
import com.apollographql.oas.select.nodes.Obj;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

public class VisitorTests {
  static final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/tmf-apis";

  private StringWriter writer;

  @BeforeEach
  void setUp() throws IOException {
    InputStream configFile = Main.class.getClassLoader().getResourceAsStream("logging.properties");

    if (configFile == null) {
      throw new IllegalArgumentException("logging.properties file not found in classpath");
    }

    // Load the configuration
    LogManager.getLogManager().readConfiguration(configFile);

    System.out.println("ParserTest.setUp creating writer...");
    this.writer = new StringWriter();
  }

  private StringWriter getWriter() {
    return writer;
  }

  @Test
  void test_001_testFullPetStoreSchema() throws URISyntaxException, IOException {
    final String source = String.format("%s/./sample-oas/petstore.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    String[] record = new String[]{
      "y", /*    visit '/pet/findByStatus'? */
      "y", /* Add all properties from Pet?:
             - name: String,
             - photoUrls: [String],
             - tags: [Tag],
             - status: String,
             - id: Int,
             - category: Category
             */
      "y", /* Add all properties from Category?:
           - name: String,
           - id: Int
           */
      "y", /* Add all properties from Tag?:
           - name: String,
           - id: Int
           */
      "y", /*    visit '/pet/findByTags'? */
      "y", /*    visit '/pet/{petId}'? */
      "n", /*    visit '/store/inventory'? */
      "y", /*    visit '/store/order/{orderId}'? */
      "y", /* Add all properties from Order?:
           - quantity: Int,
           - petId: Int,
           - shipDate: String,
           - status: String,
           - id: Int,
           - complete: Boolean
           */
      "n", /*    visit '/user/login'? */
      "n", /*    visit '/user/logout'? */
      "y", /*    visit '/user/{username}'? */
      "y", /* Add all properties from User?:
           - username: String,
           - email: String,
           - password: String,
           - lastName: String,
           - firstName: String,
           - id: Int,
           - userStatus: Int,
           - phone: String
           */
    };

    Prompt.get(Prompt.Factory.player(record));

    final Visitor visitor = new Visitor(parser);
    final Set<Type> collected = visitor.visit();
    assertNotNull(collected);
    assertEquals(5, collected.size(), "Should have collected 5 paths");
  }

  @Test
  void test_001_testMinimalPetstore() throws URISyntaxException, IOException {
    final String source = String.format("%s/./sample-oas/petstore.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    String[] record = new String[]{
      "n", /*    visit '/pet/findByStatus'? */
      "n", /*    visit '/pet/findByTags'? */
      "y", /*    visit '/pet/{petId}'? */
      "n", /* Add all properties from Pet? */
      "y", /*    add property 'PropScalar {name='id', type='Int', parent='Obj {name='#/components/schemas/Pet', children=0, props=0}'}'? */
      "y", /*    add property 'PropScalar {name='name', type='String', parent='Obj {name='#/components/schemas/Pet', children=0, props=1}'}'? */
      "n", /*    add property 'PropRef {name='category', ref='#/components/schemas/Category', parent='Obj {name='#/components/schemas/Pet', children=0, props=2}'}'? */
      "y", /*    add property 'PropArray {name='photoUrls', items='items', parent='Obj {name='#/components/schemas/Pet', children=0, props=2}'}'? */
      "n", /*    add property 'PropArray {name='tags', items='items', parent='Obj {name='#/components/schemas/Pet', children=0, props=3}'}'? */
      "y", /*    add property 'PropScalar {name='status', type='String', parent='Obj {name='#/components/schemas/Pet', children=0, props=3}'}'? */
      "n", /*    visit '/store/inventory'? */
      "n", /*    visit '/store/order/{orderId}'? */
      "n", /*    visit '/user/login'? */
      "n", /*    visit '/user/logout'? */
      "n", /*    visit '/user/{username}'? */
    };

    Prompt.get(Prompt.Factory.player(record));

    final Visitor visitor = new Visitor(parser);
    final Set<Type> collected = visitor.visit();
    assertNotNull(collected);
    assertEquals(1, collected.size(), "Should have collected 5 paths");

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    final Set<Type> childrenSet = new LinkedHashSet<>();
    findAllChildren(type, childrenSet);

    assertFalse(childrenSet.isEmpty(), "Should have found many children");
    assertEquals(3, childrenSet.size());

    final Map<String, Type> types = visitor.getContext().getTypes();
    assertEquals(1, types.size());
    assertTrue(types.containsKey("#/components/schemas/Pet"), "Should contain definition for Pet");
    assertInstanceOf(Obj.class, types.get("#/components/schemas/Pet"));
  }

  @Test
  void test_003_testTMF637_ScalarsOnly() throws IOException {
    final String source = String.format("%s/tmf-specs/TMF637-ProductInventory-v5.0.0.oas.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    String[] record = new String[]{ "n", "y", "y", "y", "n", "n", "n", "y",
      "y", "n", "y", "y", "y", "y", "n", "n", "n", "n", "n", "n", "n", "y",
      "n", "n", "n", "n", "n", "y", "n", "y"
    };

    Prompt.get(Prompt.Factory.player(record));

    final Visitor visitor = new Visitor(parser);
    final Set<Type> collected = visitor.visit();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    final RefCounter counter = new RefCounter();
    counter.count(type);

    final Map<String, Integer> values = counter.get();
    System.out.println("VisitorTests.test_003_testTMF637_ScalarsOnly -- \n" + values);
  }
  @Test
  void test_003_testTMF637_Full() throws IOException {
    final String source = String.format("%s/tmf-specs/TMF637-ProductInventory-v5.0.0.oas.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

//    String[] record = new String[]{ "n", "y", "y", "y", "n", "n", "n", "y",
//      "y", "n", "y", "y", "y", "y", "n", "n", "n", "n", "n", "n", "n", "y",
//      "n", "n", "n", "n", "n", "y", "n", "y"
//    };

    Prompt.get(Prompt.Factory.yes());

    final Visitor visitor = new Visitor(parser);
    final Set<Type> collected = visitor.visit();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

    final RefCounter counter = new RefCounter();
    counter.addAll(collected);

    final Map<String, Integer> values = counter.get();
    System.out.println("VisitorTests.test_003_testTMF637_Full -- \n" + values);
  }
  private static OpenAPI createParser(String source) {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    return new OpenAPIV3Parser().read(source, null, options);
  }

  void findAllChildren(Type type, Set<Type> childrenSet) {
    childrenSet.add(type);
    for (Type child : type.getChildren()) {
      findAllChildren(child, childrenSet);
    }
  }
}
