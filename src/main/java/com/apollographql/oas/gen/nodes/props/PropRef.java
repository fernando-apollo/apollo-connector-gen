package com.apollographql.oas.gen.nodes.props;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.Composed;
import com.apollographql.oas.gen.nodes.Obj;
import com.apollographql.oas.gen.nodes.Type;
import com.apollographql.oas.gen.nodes.Union;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

  public String path() {
    final List<Type> ancestors = getPaths(this);
    return ancestors.stream().map(t -> t.id())
      .collect(Collectors.joining(">"))
      .replaceAll("#/components/schemas", "#/c/s")
      ;
  }

  public String getRef() {
    return ref;
  }

  public Type getRefType() {
    return refType;
  }

  @Override
  public String getValue(Context context) {
    final Type type = getRefType();
    return type != null ? type.getSimpleName() : NameUtils.getRefName(getRef());
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    return super.dependencies(context);
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [prop-ref]", "in " + getName() + ", ref: " + getRef());

//    final Type cached = context.get(getRef());
//    if (cached == null) {
      final Schema schema = context.lookupRef(getRef());
      assert schema != null;

      final Type type = Factory.fromSchema(this, schema);
      this.refType = type;

      type.setName(getRef());
      type.visit(context);
//    }
//    else {
//      final Type clone = cached.clone();
//      this.refType = adopt(clone);
//    }

    if (!this.getChildren().contains(getRefType())) {
      this.add(getRefType());
    }

    setVisited(true);

    trace(context, "<- [prop-ref]", "out " + getName() + ", ref: " + getRef());
    context.leave();
  }

  // TODO
  private Type adopt(final Type clone) {
    // this would mean we would need to "re-parent" the children as well?
    return null;
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    final String fieldName = getName().startsWith("@") ? getName().substring(1) : getName();
    writer
      .append(" ".repeat(context.getStack().size()))
      .append(fieldName);

    if (needsBrackets(getRefType())) {
      writer.append(" {");
      writer.append("\n");
    }

    for (Type child : getChildren()) {
      child.select(context, writer);
    }

    if (needsBrackets(getRefType())) {
      writer
        .append(" ".repeat(context.getStack().size()))
        .append("}");

      writer.append("\n");
    }
  }

  private boolean needsBrackets(Type child) {
    return child instanceof Obj || child instanceof Union || child instanceof Composed;
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
