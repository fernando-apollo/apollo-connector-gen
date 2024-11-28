package com.apollographql.connector.oas.gen;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

public class GenObject extends Gen {
  private final String name;
  private List<GenField> fields = new LinkedList<>();

  public GenObject(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void add(GenField field) {
    if (field == null) throw new IllegalArgumentException("Field cannot be null!");
    fields.add(field);
  }

  public List<GenField> getFields() {
    return fields;
  }

  @Override
  public void generate(Writer writer) throws IOException {
    for (GenField f : getFields()) {
      if (f.getType() instanceof GenObject)
        f.getType().generate(writer);
    }

    writer.append("type ");
    writer.append(getName());
    writer.append(" {\n");

    indent++;
    for (Gen f : getFields()) {
      f.generate(writer);
      writer.write(" ".repeat(indent) + "\n");
    }
    indent--;

    writer.append("}\n\n");
  }
}
