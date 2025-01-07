package com.apollographql.oas.gen.nodes.props;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.nodes.*;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
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
    if (!isVisited()) {
      this.visit(context);
    }

    context.enter(this);
    trace(context, "-> [prop-obj:dependencies]", "in: " + getName());

    var result = Set.of(getObj());

    trace(context, "<- [prop-obj:dependencies]", "out: " + getName());
    context.leave(this);
    return result;
  }

  @Override
  public void visit(final Context context) {
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [prop-obj:visit]", "in " + getName() + ", obj: " + getObj().getSimpleName());

    getObj().visit(context);
    if (!this.getChildren().contains(getObj())) {
      this.add(getObj());
    }

    setVisited(true);

    trace(context, "<- [prop-obj:visit]", "out " + getName() + ", obj: " + getObj().getSimpleName());
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    trace(context, "-> [prop-obj:select]", "in " + getName() + ", obj: " + getObj().getSimpleName());

    final String fieldName = getName();
    final String sanitised = NameUtils.sanitiseFieldForSelect(fieldName);

    writer
      .append(" ".repeat(context.getIndent() + context.getStack().size()))
      .append(sanitised);

    if (needsBrackets(getObj())) {
      writer.append(" {");
      writer.append("\n");
      context.enter(this);;
    }

    for (Type child : getChildren()) {
      child.select(context, writer);
    }

    if (needsBrackets(getObj())) {
      context.leave(this);
      writer
        .append(" ".repeat(context.getIndent() + context.getStack().size()))
        .append("}");

      writer.append("\n");
    }

    trace(context, "<- [prop-obj:select]", "out " + getName() + ", obj: " + getObj().getSimpleName());
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
