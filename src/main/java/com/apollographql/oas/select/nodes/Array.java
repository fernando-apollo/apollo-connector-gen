package com.apollographql.oas.select.nodes;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.select.log.Trace.trace;

public class Array extends Type {
  private final Schema items;
  private Type itemsType;

  public Array(final Type parent, final Schema items) {
    super(parent, "items");
    this.items = items;
  }

  public Schema getItems() {
    return items;
  }

  public Type getItemsType() {
    return itemsType;
  }

  @Override
  public String id() {
    return "array://" + getItemsType().getName();
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context,"-> [array]", "in");

    if (itemsType == null) {
      itemsType = Factory.fromSchema(context, this, getItems());
      assert itemsType != null;

      trace(context, "   [array]", "type: " + itemsType);
      itemsType.visit(context);
      setVisited(true);
    }

    trace(context,"-> [array]", "out");
    context.leave();
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [array::generate]", String.format("-> in: %s", this.getSimpleName()));

    writer.append("[");
    getItemsType().generate(context, writer);
    writer.append("]");

    trace(context, "<- [array::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave();
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [array::select]", String.format("-> in: %s", this.getSimpleName()));

    getItemsType().select(context, writer);

    trace(context, "<- [array::select]", String.format("-> out: %s", this.getSimpleName()));
    context.leave();
  }

  @Override
  public String getSimpleName() {
    return getItems().getClass().getSimpleName();
  }

  @Override
  public String toString() {
    return "Array {" +
      "name='" + name + '\'' +
      ", items=" + getSimpleName() +
      ", children=" + children.size() +
      ", props=" + props.size() +
      '}';
  }

}
