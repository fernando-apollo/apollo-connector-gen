package com.apollographql.oas.select.nodes.props;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.apollographql.oas.select.log.Trace.trace;

public class PropArray extends Prop {
  private final Prop items;

  public PropArray(Type parent, String name, Schema schema, Prop items) {
    super(parent, name, schema);
    this.items = items;

    if (items == null) {
      throw new IllegalArgumentException("Items type cannot be null for array!");
    }
  }

  public Prop getItems() {
    return items;
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context,"-> [prop-array]", "in");

    trace(context,"   [array]", "type: " + getItems());
    getItems().visit(context);
    setVisited(true);

    trace(context,"<- [array]", "out");
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    final String fieldName = getName().startsWith("@") ? getName().substring(1) : getName();
    writer
      .append(" ".repeat(context.getStack().size()))
      .append(fieldName);

    if (needsBrackets(getItems())) {
      writer.append(" {");
      writer.append("\n");
    }

    // the array is a bit special because we add an intermediate "items" type, therefore we
    // need to fetch the type of that one directly
    for (Type child : getItems().getChildren()) {
      child.select(context, writer);
    }

    if (needsBrackets(getItems())) {
      writer
        .append(" ".repeat(context.getStack().size()))
        .append("}");
    }
    writer.append("\n");
  }

  public Set<Type> dependencies(final Context context) {
    if (!isVisited()) {
      this.visit(context);
    }

    final Set<Type> set = new HashSet<>();
    set.add(getItems());
    return set;
  }

  @Override
  public String getSimpleName() {
    return getItems().getClass().getSimpleName();
  }

  public String forPrompt(final Context context) {
    return getName() + ": " + getValue(context);
  }

  @Override
  public String toString() {
    return "PropArray {" +
      "name='" + getName() + '\'' +
      ", items='" + getItems().getName() + '\'' +
      ", parent='" + getParent() + '\'' +
      '}';
  }

  @Override
  public String getValue(Context context) {
    return "[" + getItems().getValue(context) + "]";
  }

  private boolean needsBrackets(Type child) {
    return child instanceof PropRef;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    final PropArray propArray = (PropArray) o;
    return Objects.equals(items, propArray.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), items);
  }
}
