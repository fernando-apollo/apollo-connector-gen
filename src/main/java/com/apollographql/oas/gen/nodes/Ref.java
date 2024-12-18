package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.props.Prop;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import static com.apollographql.oas.gen.log.Trace.trace;

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
    return "ref:" + getRef();
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [ref]", "in: " + getRef());

//    final Type cached = context.get(getRef());
//    if (cached == null) {
    final Schema schema = context.lookupRef(getRef());
    assert schema != null;

    final Type type = Factory.fromSchema(this, schema);
    assert type != null;
    this.refType = type;
//    }
//    else {
//      this.refType = cached;
//    }

    this.refType.setName(getRef());
    this.refType.visit(context);

    setVisited(true);

    trace(context, "<- [ref]", "out: " + getRef());
    context.leave();
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [ref::generate]", String.format("-> in: %s", this.getSimpleName()));

    if (context.inContextOf(Response.class, this) && getRefType() instanceof Array) {
      writer.append("[").append(getFirstChild().getName()).append("]");
    }
    else {
      writer.write(NameUtils.getRefName(getRef()));
    }

    trace(context, "<- [ref::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave();
  }

  private Type getFirstChild() {
    return getRefType().getChildren().get(0);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [ref::select]", String.format("-> in: %s", this.getSimpleName()));

    getRefType().select(context, writer);

    trace(context, "<- [ref::select]", String.format("-> out: %s", this.getSimpleName()));
    context.leave();
  }

  @Override
  public Map<String, Prop> getProps() {
    return getRefType().getProps();
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
