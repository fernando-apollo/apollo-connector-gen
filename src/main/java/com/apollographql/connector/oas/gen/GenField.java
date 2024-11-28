package com.apollographql.connector.oas.gen;

import java.io.IOException;
import java.io.Writer;

public class GenField extends Gen {
  private final String name;
  private final Gen type;

  public String getName() {
    return name;
  }

  public Gen getType() {
    return type;
  }

  public GenField(String name, Gen type) {
    this.name = name;
    this.type = type;
  }

  public void generate(Writer writer) throws IOException {
    if (getType() instanceof GenObject) {
      getType().generate(writer);
    }

    writer.write(" ".repeat(indent) + getName() + ": ");
  }
}
