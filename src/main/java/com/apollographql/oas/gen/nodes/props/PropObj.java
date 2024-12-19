package com.apollographql.oas.gen.nodes.props;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.nodes.*;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;

import static com.apollographql.oas.gen.log.Trace.trace;

public class PropObj extends Prop implements Cloneable {
  private final Type obj;

  public PropObj(final Type parent, final String propertyName, final Schema schema, final Type obj) {
    super(parent, propertyName, schema);
    this.obj = obj;
  }

  public Type getObj() {
    return obj;
  }

  @Override
  public String id() {
    return "prop:obj:" + getName();
  }

  @Override
  public String getName() {
    if (this.name == null) {
      final Type parent = getParent();
      final String parentName = parent.getName();

      if (parent instanceof PropRef) {
        this.name = parentName.replace("ref:", "obj:");
      }
      else {
        this.name = "[prop:obj:" + hashCode() + "]";
      }
    }

    return this.name;
  }

  @Override
  public String getValue(Context context) {
    return getName();
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    return Set.of(getObj());
  }

//  @Override
//  public void generate(final Context context, final Writer writer) throws IOException {
//    context.enter(this);
//    trace(context, "-> [prop:obj::generate]", String.format("-> in: %s", this.getName()));
//
//    writer.append("type ")
//      .append(NameUtils.getRefName(getName()))
//      .append(" {\n");
//
//    for (Prop prop : this.getProps().values()) {
//      trace(context, "-> [prop:obj::generate]", String.format("-> property: %s (parent: %s)", prop.getName(), prop.getParent().getSimpleName()));
//      prop.generate(context, writer);
//    }
//
//    writer.append("}\n\n");
//
//    trace(context, "<- [prop:obj::generate]", String.format("-> out: %s", this.getName()));
//    context.leave();
//  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [prop-obj]", "in " + getName() + ", obj: " + getObj().getSimpleName());

    getObj().visit(context);
    if (!this.getChildren().contains(getObj())) {
      this.add(getObj());
    }

    setVisited(true);

    trace(context, "<- [prop-obj]", "out " + getName() + ", obj: " + getObj().getSimpleName());
    context.leave();
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    final String fieldName = getName();
    final String sanitised = NameUtils.sanitiseFieldForSelect(fieldName);

    writer
      .append(" ".repeat(context.getStack().size()))
      .append(sanitised);

    if (needsBrackets(getObj())) {
      writer.append(" {");
      writer.append("\n");
    }

    for (Type child : getChildren()) {
      child.select(context, writer);
    }

    if (needsBrackets(getObj())) {
      writer
        .append(" ".repeat(context.getStack().size()))
        .append("}");

      writer.append("\n");
    }
    context.leave();
  }

  private boolean needsBrackets(Type child) {
    return child instanceof Obj || child instanceof Union || child instanceof Composed;
  }

  public String forPrompt(final Context context) {
    return getName() + ": " + NameUtils.getRefName(getObj().getName());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    final PropObj propRef = (PropObj) o;
    return Objects.equals(obj, propRef.obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), obj);
  }

  @Override
  public String toString() {
    return "PropRef {" +
      "name='" + getName() + '\'' +
      ", obj='" + getObj() + '\'' +
      ", parent='" + getParent() + '\'' +
      '}';
  }
}
