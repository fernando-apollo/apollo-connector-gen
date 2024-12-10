package com.apollographql.oas.select;

import com.apollographql.oas.converter.Main;
import com.apollographql.oas.select.context.RefCounter;
import com.apollographql.oas.select.nodes.Obj;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.nodes.props.Prop;
import com.apollographql.oas.select.nodes.props.PropArray;
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
  void test_001_testMinimalPetstore() throws IOException {
    loadRecording("test_001_testMinimalPetstore.txt");

    final OpenAPI parser = createParser(String.format("%s/./sample-oas/petstore.yaml", baseURL));
    assertNotNull(parser);

    final Visitor visitor = new Visitor(parser);
    visitor.visit();

    final Set<Type> collected = visitor.getCollected();
    assertNotNull(collected);
    assertEquals(1, collected.size(), "Should have collected 5 paths");

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    final Set<Type> childrenSet = new LinkedHashSet<>();
    findAllChildren(type, childrenSet);

    assertFalse(childrenSet.isEmpty(), "Should have found many children");
    assertEquals(7, childrenSet.size());

    final Map<String, Type> types = visitor.getContext().getTypes();
    assertEquals(1, types.size());
    assertTrue(types.containsKey("#/components/schemas/Pet"), "Should contain definition for Pet");
    assertInstanceOf(Obj.class, types.get("#/components/schemas/Pet"));

    printSchema(visitor);
  }

  @Test
  void test_002_testFullPetStoreSchema() throws URISyntaxException, IOException {
    loadRecording("test_001_FullPetstoreSchema.txt");

    final String source = String.format("%s/./sample-oas/petstore.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    final Visitor visitor = new Visitor(parser);
    visitor.visit();
    assertNotNull(visitor.getCollected());
    assertEquals(3, visitor.getCollected().size(), "Should have collected 3 paths: " + visitor.getCollected());

    final Map<String, Type> types = visitor.getContext().getTypes();
    assertEquals(3, types.size());
    assertTrue(types.containsKey("#/components/schemas/Pet"), "Should contain definition for Pet");
    assertTrue(types.containsKey("#/components/schemas/Category"), "Should contain definition for Category");
    assertTrue(types.containsKey("#/components/schemas/Tag"), "Should contain definition for Tag");

    assertInstanceOf(Obj.class, types.get("#/components/schemas/Pet"));
    assertInstanceOf(Obj.class, types.get("#/components/schemas/Category"));
    assertInstanceOf(Obj.class, types.get("#/components/schemas/Tag"));

    final Prop tags = types.get("#/components/schemas/Pet").getProps().get("tags");
    assertInstanceOf(PropArray.class, tags);

    printSchema(visitor);
  }

  @Test
  void test_003_testConsumerJourney() throws IOException {
    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/poc/services";
    final String source = String.format("%s/js-mva-consumer-info_v1.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    loadRecording("test_001_ConsumerJourney.txt");

    final Visitor visitor = new Visitor(parser);
    visitor.visit();
    final Set<Type> collected = visitor.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

//    final RefCounter counter = new RefCounter();
//    counter.addAll(collected);
//    printRefs(counter.getCount());
//
    System.out.println(" ----------- schema -------------- ");
    printSchema(visitor);
  }

  @Test
  void test_TMF633_IntentOrValue_to_Union() throws IOException {
    loadRecording("TMF633_IntentOrValue_to_Union.txt");

    final String source = String.format("%s/tmf-specs/TMF637-001-UnionTest.yaml", baseURL);
    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    final Visitor visitor = new Visitor(parser);
    visitor.visit();
    final Set<Type> collected = visitor.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    // TODO: add assertions
//    final RefCounter counter = new RefCounter();
//    counter.addAll(collected);
//    printRefs(counter.getCount());

    System.out.println(" ----------- schema -------------- ");
    printSchema(visitor);
  }

  @Test
  void test_TMF637_001_ComposedTest() throws IOException {
    loadRecording("test_TMF637_001_ComposedTest.txt");

    final String source = String.format("%s/tmf-specs/TMF637-001-ComposedTest.yaml", baseURL);
    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    final Visitor visitor = new Visitor(parser);
    visitor.visit();

    final Set<Type> collected = visitor.getCollected();
    assertNotNull(collected);

    final Map<String, Type> types = visitor.getContext().getTypes();
    assertTrue(types.containsKey("#/components/schemas/Product"));
//    assertTrue(types.containsKey("#/components/schemas/BillingAccountRef"));
    assertTrue(types.containsKey("#/components/schemas/Product"));

    System.out.println(" ----------- schema -------------- ");
    printSchema(visitor);
  }

  @Test
  void test_003_testTMF637_Full() throws IOException {
    final String source = String.format("%s/tmf-specs/TMF637-ProductInventory-v5.0.0.oas.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    Prompt.get(Prompt.Factory.yes());

    final Visitor visitor = new Visitor(parser);
    visitor.visit();
    final Set<Type> collected = visitor.getCollected();
      assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

//    final RefCounter counter = new RefCounter();
//    counter.addAll(collected);
//    System.out.println("VisitorTests.test_003_testTMF637_Full -- \n");
//    final Map<String, Integer> values = counter.getCount();
//    printRefs(values);

    System.out.println("VisitorTests.test_003_testTMF637_Full ----------- schema -------------- \n");
    printSchema(visitor);
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

  private static void printSchema(final Visitor visitor) throws IOException {
    final StringWriter writer = new StringWriter();
    visitor.writeSchema(writer);
    System.out.println(" ----------- schema -------------- ");
    System.out.println(writer);
  }

  private static void loadRecording(final String resource) {
    InputStream input = VisitorTests.class.getClassLoader()
      .getResourceAsStream(resource);
    assertNotNull(input);

    final String[] recording = Recordings.fromInputStream(input);
    assertNotNull(recording);
    assertTrue(recording.length > 0);

    Prompt.get(Prompt.Factory.player(recording));
  }
}
