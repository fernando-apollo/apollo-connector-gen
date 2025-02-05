package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.naming.Naming;
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
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [ref:visit]", "in: " + getRef());

    final Schema schema = context.lookupRef(getRef());
    assert schema != null;

    this.refType = Factory.fromSchema(this, schema);
    this.refType.setName(getRef());
    this.refType.visit(context);

    setVisited(true);

    trace(context, "<- [ref:visit]", "out: " + getRef());
    context.leave(this);
  }

  private void setRefType(final Type refType) {
    this.refType = refType;
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [ref::generate]", String.format("-> in: %s", this.getSimpleName()));

    if (context.inContextOf(Response.class, this) && getRefType() instanceof Array) {
      writer.append("[").append(getFirstChild().getName()).append("]");
    }
    else {
      // apparently we can have terrible names like 'catalog-data-product-search-results-interface', so let's
      // rewrite those to something more sensible
      final String sanitised = Naming.genTypeName(getName());
      final String refName = Naming.getRefName(getName());

      writer.write(sanitised.equals(refName) ? refName : sanitised);
    }

    trace(context, "<- [ref::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  private Type getFirstChild() {
    return getRefType().getChildren().get(0);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    trace(context, "-> [ref::select]", String.format("-> in: %s", this.getSimpleName()));
    getRefType().select(context, writer);
    trace(context, "<- [ref::select]", String.format("-> out: %s", this.getSimpleName()));
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
