package com.apollographql.oas.select.nodes.props;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.converter.utils.Trace.print;
import static com.apollographql.oas.select.log.Trace.trace;

public class PropScalar extends Prop {
  protected final String type;

  public PropScalar(final Type parent, final String name, final String type, final Schema schema) {
    super(parent, name, schema);
    this.type = type;
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [prop-scalar]", "in " + getName() + ", type: " + getType());

    final Type type = Factory.fromSchema(context, this, getSchema());
    type.visit(context);

    trace(context, "<- [prop-scalar]", "out " + getName() + ", type: " + getType());
    context.leave(this);
  }

  @Override
  public String getValue(Context context) {
    return getType();
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    super.select(context, writer);
  }

  @Override
  public String toString() {
    return "PropScalar {" +
      "name='" + getName() + '\'' +
      ", type='" + getType() + '\'' +
      ", entity='" + getParent().getName() + '\'' +
      '}';
  }

  public String getType() {
    return type;
  }
}
