package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.naming.Naming;
import io.swagger.v3.oas.models.media.Schema;

import static com.apollographql.oas.gen.log.Trace.trace;

public class ResponseRef extends Type {
  private final String ref;

  private Type refType;

  public ResponseRef(final Type parent, final String ref) {
    super(parent, Naming.getRefName(ref));
    this.ref = ref;
  }

  public String getRef() {
    return ref;
  }

  @Override
  public void visit(final Context context) {
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [response-ref:visit]", "in");

    final Type cached = context.get(getRef());
    if (cached == null) {
      final Schema schema = context.lookupRef(getRef());
      assert schema != null;

      final Type type = Factory.fromSchema(this, schema);
      this.refType = type;

      type.setName(getRef());
      type.visit(context);
    }
    else {
      this.refType = cached;
    }

    trace(context, "-> [response-ref:visit]", "out");
    context.leave(this);
  }

  public Type getRefType() {
    return refType;
  }

  public void setRefType(final Type refType) {
    this.refType = refType;
  }
}
