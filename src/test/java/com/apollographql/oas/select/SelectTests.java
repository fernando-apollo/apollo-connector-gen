package com.apollographql.oas.select;

import com.apollographql.oas.converter.Walker;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.prompt.Input;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.apollographql.oas.select.log.Trace.trace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SelectTests {
  private StringWriter writer;

  @BeforeEach
  void setUp() {
    System.out.println("ParserTest.setUp creating writer...");
    this.writer = new StringWriter();
  }

  private StringWriter getWriter() {
    return writer;
  }

  @Test
  void test_001_testFullPetStoreSchema() throws URISyntaxException, IOException {
    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/tmf-apis";
    final String source = String.format("%s/./sample-oas/petstore.yaml", baseURL);

    final OpenAPI parser = createParser(source);
    assertNotNull(parser);

    String[] record = new String[] {
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
  private static OpenAPI createParser(String source) {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    return new OpenAPIV3Parser().read(source, null, options);
  }

}
