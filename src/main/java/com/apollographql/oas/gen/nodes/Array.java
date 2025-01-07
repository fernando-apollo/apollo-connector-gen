package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.gen.log.Trace.trace;

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
    return "array:" + (getItemsType() != null ? getItemsType().getName() : "unknown-yet");
  }

  @Override
  public void visit(final Context context) {
    if (!context.enter(this) || isVisited()) return;
    trace(context,"-> [array:visit]", "in");

    if (itemsType == null) {
      itemsType = Factory.fromSchema(this, getItems());
      assert itemsType != null;

      trace(context, "   [array:visit]", "type: " + itemsType);
      itemsType.visit(context);
      setVisited(true);
    }

    trace(context,"-> [array:visit]", "out");
    context.leave(this);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [array::generate]", String.format("-> in: %s", this.getSimpleName()));

    writer.append("[");
    getItemsType().generate(context, writer);
    writer.append("]");

    trace(context, "<- [array::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [array::select]", String.format("-> in: %s", this.getSimpleName()));

    getItemsType().select(context, writer);

    trace(context, "<- [array::select]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
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
