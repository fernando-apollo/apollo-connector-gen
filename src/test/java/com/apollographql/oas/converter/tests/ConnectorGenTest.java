package com.apollographql.oas.converter.tests;

import com.apollographql.connector.oas.ConnectorGen;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConnectorGenTest {
  @Test
  public void test_001_genPetstoreSpec() throws Exception {
    final Path path = Paths.get(getClass().getClassLoader().getResource("petstore.yaml").toURI());
    assertNotNull(path);

    final ConnectorGen generator = ConnectorGen.create(path.toString());
    final String schema = generator.operations(new String[] {"/pet/{petId}"});

    assertNotNull(schema, "Generated schema should not be null!");
  }
}
