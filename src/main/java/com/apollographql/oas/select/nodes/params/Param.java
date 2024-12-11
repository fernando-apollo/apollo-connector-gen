package com.apollographql.oas.select.nodes.params;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.select.log.Trace.trace;

public class Param extends Type {

  private final Schema schema;
  private final boolean required;
  private final Object defaultValue;

  private Type resultType;

  public Param(Type parent, String name, Schema schema, boolean required, Object defaultValue ) {
    super(parent, name);
    this.schema = schema;
    this.required = required;
    this.defaultValue = defaultValue;
  }

  public Schema getSchema() {
    return schema;
  }

  public boolean isRequired() {
    return required;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public Type getResultType() {
    return resultType;
  }

  public void setResultType(Type resultType) {
    this.resultType = resultType;
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [param]", "in: " + getName());

    this.resultType = Factory.fromSchema(this, getSchema());
    trace(context, "   [param]", "type: " + resultType);
    this.resultType.visit(context);

    trace(context, "<- [param]", "out: " + getName());
    context.leave();
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [param::generate]", String.format("-> in: %s", this.getSimpleName()));

    writer.write(NameUtils.genParamName(getName()));
    writer.write(": ");

    getResultType().generate(context, writer);

    if (isRequired()) {
      writer.write("!");
    }

    if (getDefaultValue() != null) { // best effort here..
      writer.write(" = \"");
      writer.write(getDefaultValue().toString());
      writer.write("\"");
    }

    trace(context, "<- [param::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave();
  }

  @Override
  public String toString() {
    return "Param{" +
      "name=" + getName() +
      ", required=" + required +
      ", defaultValue=" + defaultValue +
      ", props=" + props +
      ", resultType=" + resultType +
      '}';
  }
}
