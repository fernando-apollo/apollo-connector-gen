package com.apollographql.connector.oas.gen;

import java.io.IOException;
import java.io.Writer;

public abstract class Gen {
  protected int indent = 0;
  public abstract void generate(Writer writer) throws IOException;
}
