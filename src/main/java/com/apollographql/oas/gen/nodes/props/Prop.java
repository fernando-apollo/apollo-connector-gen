package com.apollographql.oas.gen.nodes.props;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

public abstract class Prop extends Type {
  protected final Schema schema;
  protected boolean required;

  public Prop(final Type parent, final String name, final Schema schema) {
    super(parent, name);
    this.schema = schema;
  }

  public Schema getSchema() {
    return this.schema;
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
      }
      else {
        writer.append("  \"").append(description).append("\"\n");
      }
    }

    // some fields start with '@' like '@type' -- need to remove the '@' for GQL
    final String fieldName = getName().startsWith("@") ? getName().substring(1) : getName();
    writer.append("  ").append(fieldName).append(": ");//.append(NameUtils.getRefName(type));

    writer.append(getValue(context));

    if (isRequired())
      writer.append("!");

    // TODO: source
//    writer.append(" # ").append(parent);

    writer.append("\n");
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    final String fieldName = getName().startsWith("@") ? getName().substring(1) : getName();
    writer
      .append(" ".repeat(context.getStack().size()))
      .append(fieldName)
//      .append(" # ").append(getClass().getSimpleName())
      .append("\n");

    for (Type child : getChildren()) {
      child.select(context, writer);
    }
  }
}
