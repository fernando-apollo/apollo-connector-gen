package com.apollographql.connector.oas.gen;

import java.io.IOException;
import java.io.Writer;

public class GenArray extends Gen {
  private final Gen items;

  public GenArray(Gen items) {
    super();
    this.items = items;
  }

  public Gen getItems() {
    return items;
  }

  @Override
  public void generate(Writer writer) throws IOException {
    writer.write("[");
    getItems().generate(writer);
    writer.write("]");
  }
}
