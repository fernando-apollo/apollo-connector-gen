package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.converter.utils.GqlUtils;
import com.apollographql.oas.gen.context.Context;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.gen.log.Trace.trace;

public class Scalar extends Type {
  public Scalar(final Type parent, final Schema schema) {
    super(parent, GqlUtils.getGQLScalarType(schema));
  }

  @Override
  public String id() {
    return "scalar:" + name;
  }

  @Override
  public void visit(final Context context) {
    // do nothing - scalars don't visit others
    setVisited(true);
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [scalar::generate]", String.format("-> in: %s", this.getSimpleName()));

    writer.write(getName());

    trace(context, "<- [scalar::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
//    writer.write(">>>>> SCALAR");
  }

  @Override
  public String toString() {
    return "Scalar {" +
      "name='" + name + '\'' +
      '}';
  }

}
