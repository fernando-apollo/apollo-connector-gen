package com.apollographql.connector.oas;

import com.apollographql.connector.oas.visitor.PathsVisitor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class ConnectorGen {
  private final String path;
  private OpenAPI parser;

  public ConnectorGen(String path) {
    this.path = path;
  }

  public static void main(String[] args) {
    System.out.println("ConnectorGen.main");
  }

  public static ConnectorGen create(String filePath) {
    final ConnectorGen gen = new ConnectorGen(filePath);
    gen.tryParse();
    return gen;
  }

  private void tryParse() {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    this.parser = new OpenAPIV3Parser().read(this.path, null, options);
  }

  public OpenAPI getParser() {
    return parser;
  }

  public String operations(String[] paths) {
    // TODO: iterate

    final PathsVisitor visitor = new PathsVisitor(parser, paths[0]);
    String schema = visitor.visit();

    return schema;
  }
}
