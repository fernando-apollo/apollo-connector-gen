package com.apollographql.oas.gen.nodes.props;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.*;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

import static com.apollographql.oas.gen.log.Trace.trace;

public class PropRef extends Prop implements Cloneable {
  private final String ref;
  private Type refType;

  public PropRef(Type parent, String name, Schema schema, String ref) {
    super(parent, name, schema);
    this.ref = ref;
  }

  @Override
  public String id() {
    return "prop:ref:" + getRef();
  }

  public String getRef() {
    return ref;
  }

  public Type getRefType() {
    return refType;
  }

  private void setRefType(final Type type) {
    this.refType = type;
  }

  @Override
  public String getValue(Context context) {
    final Type type = getRefType();
    return type != null ? type.getSimpleName() : NameUtils.getRefName(getRef());
  }

  @Override
  public void add(final Type child) {
    child.setName(getRef());

    final List<Type> paths = Type.getAncestors(this);
    final boolean contains = paths.contains(child);
    trace(null, "-> [prop-ref:add]", "contains child? " + contains);

    if (contains) {
      final Type ancestor = paths.get(paths.indexOf(child));
      final Type wrapper = Factory.fromCircularRef(this, ancestor);
      super.add(wrapper);

      setVisited(true);
      setRefType(wrapper);
    }
    else {
      super.add(child);
    }
  }

  @Override
  public void visit(final Context context) {
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [prop-ref:visit]", "in " + getName() + ", ref: " + getRef());

    final Schema schema = context.lookupRef(getRef());
    assert schema != null;

    final Type type = Factory.fromSchema(this, schema);

    if (refType == null) {
      this.refType = type;
      type.setName(getRef());
      type.visit(context);

      if (!this.getChildren().contains(getRefType())) {
        this.add(getRefType());
      }

      setVisited(true);
    }

    trace(context, "<- [prop-ref:visit]", "out " + getName() + ", ref: " + getRef());
    context.leave(this);
  }

  /* Unfortunately we cannot delegate this to the subtype, otherwise the entire type would
   * be generated. Therefore, we only have the option to generate it ourselves  */
  protected void generateValue(final Context context, final Writer writer) throws IOException {
    final Type type = getRefType();
    if (type instanceof Array) {
      writer.append("[");
      Type items = ((Array) type).getItemsType();
      writer.append(items.getName());
      writer.append("]");
    }
    else {
      writer.append(getValue(context));
    }
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    trace(context, "-> [prop-ref:select]", "in " + getName() + ", ref: " + getRef());

    final String fieldName = getName();
    final String sanitised = NameUtils.sanitiseFieldForSelect(fieldName);

    writer
      .append(" ".repeat(context.getIndent() + context.getStack().size()))
      .append(sanitised);

    if (needsBrackets(getRefType())) {
      writer.append(" {");
      writer.append("\n");
      context.enter(this);
    }

    for (Type child : getChildren()) {
      child.select(context, writer);
    }

    if (needsBrackets(getRefType())) {
      context.leave(this);
      writer
        .append(" ".repeat(context.getIndent() + context.getStack().size()))
        .append("}");

      writer.append("\n");
    }

    trace(context, "<- [prop-ref:select]", "out " + getName() + ", ref: " + getRef());
  }

  private boolean needsBrackets(Type child) {
    if (child instanceof Array) {
      final Array array = (Array) child;
      return needsBrackets(array.getItemsType());
    }

    return child instanceof Obj || child instanceof Union || child instanceof Composed || child instanceof CircularRef;
  }

  public String forPrompt(final Context context) {
    return getName() + ": " + NameUtils.getRefName(getRef());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    final PropRef propRef = (PropRef) o;
    return Objects.equals(ref, propRef.ref);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), ref);
  }

  @Override
  public String toString() {
    return "PropRef {" +
      "name='" + getName() + '\'' +
      ", ref='" + getRef() + '\'' +
      ", parent='" + getParent() + '\'' +
      '}';
  }
}
