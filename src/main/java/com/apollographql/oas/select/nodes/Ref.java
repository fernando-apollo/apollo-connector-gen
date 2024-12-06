package com.apollographql.oas.select.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.select.log.Trace.trace;

public class Ref extends Type {
  private final String ref;
  private Type refType;

  public Ref(final Type parent, final String name, final String ref) {
    super(parent, name);
    this.ref = ref;
  }

  public String getRef() {
    return ref;
  }

  public Type getRefType() {
    return refType;
  }

  @Override
  public String id() {
    return "ref://" + getRef();
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [ref]", "in: " + getRef());

    final Type cached = context.get(getRef());
    if (cached == null) {
      final Schema schema = context.lookupRef(getRef());
      assert schema != null;

      final Type type = Factory.fromSchema(context, this, schema);
      assert type != null;
      this.refType = type;

      type.setName(getRef());
      type.visit(context);
    }
    else {
      this.refType = cached;
    }

    trace(context, "<- [ref]", "out: " + getRef());
    context.leave(this);
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [ref::generate]", String.format("-> in: %s", this.getSimpleName()));

    writer.write(NameUtils.getRefName(getRef()));

    trace(context, "<- [ref::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [ref::select]", String.format("-> in: %s", this.getSimpleName()));

    getRefType().select(context, writer);

    trace(context, "<- [ref::select]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  @Override
  public String toString() {
    return "Ref {" +
      "name='" + name + '\'' +
      ", children=" + children.size() +
      ", props=" + props.size() +
      '}';
  }
}
