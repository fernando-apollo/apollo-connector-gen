package com.apollographql.oas.gen.nodes.props;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.naming.Naming;
import com.apollographql.oas.gen.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.gen.log.Trace.trace;

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

    writer.append("  ")
      .append(Naming.sanitiseField(getName()))
      .append(": ");

    generateValue(context, writer);

    if (isRequired())
      writer.append("!");

    // TODO: source
//    writer.append(" # ").append(parent);

    writer.append("\n");
  }

  protected void generateValue(final Context context, final Writer writer) throws IOException {
    writer.append(getValue(context));
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    trace(context, "   [prop:select]", getName());
    final String fieldName = getName();
    final String sanitised = Naming.sanitiseFieldForSelect(fieldName);
    writer
      .append(" ".repeat(context.getIndent() + context.getStack().size()))
      .append(sanitised)
      .append("\n");

    for (Type child : getChildren()) {
      child.select(context, writer);
    }
  }
}
