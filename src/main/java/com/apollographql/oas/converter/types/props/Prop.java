package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

public abstract class Prop {
  protected final String name;
  protected final String source;
  protected final Schema schema;
  protected boolean required;

  public Prop(final String name, final String source, final Schema schema) {
    this.name = name;
    this.source = source;
    this.schema = schema;
  }

  public String getName() {
    return name;
  }

  public Schema getSchema() {
    return this.schema;
  }

  public String getSource() {
    return source;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isRequired() {
    return required;
  }

  public abstract String getValue(Context context);

  public void generate(Context context, Writer writer) throws IOException {
    String description = this.getSchema().getDescription();

    if (description != null) {
      if (description.contains("\n") || description.contains("\r") || description.contains("\"")) {
        writer.append("  \"\"\"\n").append("  ").append(description).append("\n  \"\"\"\n");
      } else {
        writer.append("  \"").append(description).append("\"\n");
      }
    }

    // some fields start with '@' like '@type' -- need to remove the '@' for GQL
    final String fieldName = getName().startsWith("@") ? getName().substring(1) : getName();
    writer.append("  ").append(fieldName).append(": ");//.append(NameUtils.getRefName(type));

    writer.append(getValue(context));

    if (isRequired())
      writer.append("!");

    if (source != null)
      writer.append(" # ").append(NameUtils.getRefName(source));

    writer.append("\n");
  }

  public abstract void select(Context context, Writer writer, Stack<CType> stack) throws IOException;
}
