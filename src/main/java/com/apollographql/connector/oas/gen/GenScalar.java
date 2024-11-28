package com.apollographql.connector.oas.gen;

import java.io.IOException;
import java.io.Writer;

public class GenScalar extends Gen {
  private final String name;
  private final String type;

  public GenScalar(String name, String value) {
    super();
    this.name = name;
    this.type = value;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  @Override
  public void generate(Writer writer) throws IOException {
    writer.write(getType());
  }
}
