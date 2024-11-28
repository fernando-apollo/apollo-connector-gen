
package com.apollographql.oas.converter.tests;

import com.apollographql.oas.converter.Walker;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ParserTest {

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
  void test_001_testPetStoreSchema() throws URISyntaxException, IOException {
    final Path path = Paths.get(getClass().getClassLoader().getResource("petstore.yaml").toURI());
    assertNotNull(path);

    final OpenAPI parser = createParser(path.toString());

    final Walker walker = new Walker(parser);
    walker.walk();
    walker.generate(getWriter());
    System.out.println("ParserTest.test_001_testPetStoreSchema -> " + getWriter().toString());
  }

  private static OpenAPI createParser(String source) {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    return new OpenAPIV3Parser().read(source, null, options);
  }
}
