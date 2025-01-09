package com.apollographql.oas.gen.nodes.params;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.gen.log.Trace.trace;

public class Param extends Type {

  private final Schema schema;
  private final boolean required;
  private final Object defaultValue;
  private final Parameter parameter;

  private Type resultType;

  public Param(Type parent, String name, Schema schema, boolean required, Object defaultValue, Parameter parameter) {
    super(parent, name);
    this.schema = schema;
    this.required = required;
    this.defaultValue = defaultValue;
    this.parameter = parameter;
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

  public String getIn() {
    return parameter.getIn();
  }

  @Override
  public void visit(final Context context) {
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [param:visit]", "in: " + getName());

    this.resultType = Factory.fromSchema(this, getSchema());
    trace(context, "   [param:visit]", "type: " + resultType);
    this.resultType.visit(context);

    trace(context, "<- [param:visit]", "out: " + getName());
    context.leave(this);
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
      writeDefaultValue(writer);
    }

    trace(context, "<- [param::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  private void writeDefaultValue(final Writer writer) throws IOException {
    writer.write(" = ");
    final Object value = getDefaultValue();

    if (value instanceof Number)
      writer.write(value.toString());

    if (value instanceof String) {
      writer.write('"');
      writer.write(String.valueOf(value));
      writer.write('"');
    }
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
