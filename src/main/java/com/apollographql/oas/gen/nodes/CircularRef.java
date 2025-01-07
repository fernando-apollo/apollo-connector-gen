package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.gen.context.Context;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

import static com.apollographql.oas.gen.log.Trace.trace;

public class CircularRef extends Type {
  private final Type child;

  public CircularRef(final Type parent, final Type child) {
    super(parent, child.getName());
    this.child = child;
  }

  public Type getChild() {
    return child;
  }

  @Override
  public void visit(final Context context) {
    trace(context, "-> [circular-ref:visit]", String.format("-> in: %s", getSimpleName()));
    // do nothing, this type is always visited
    trace(context, "<- [circular-ref:visit]", String.format("-> out: %s", getSimpleName()));
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    trace(context, "-> [circular-ref:dependencies]", String.format("-> in: %s", getSimpleName()));
    final Set<Type> result = Collections.emptySet();
    trace(context, "<- [circular-ref:dependencies]", String.format("-> out: %s", getSimpleName()));
    return result;
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    trace(context, "-> [circular-ref:generate]", String.format("-> in: %s", getSimpleName()));
    // do nothing, we can't really generate a circular reference
    trace(context, "<- [circular-ref:generate]", String.format("-> out: %s", getSimpleName()));
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
//    context.enter(this);
    trace(context, "-> [circular-ref:select]", String.format("-> in: %s", getSimpleName()));

    // do nothing, we can't really select a circular reference
    writer
      .append(" ".repeat(context.getIndent() + context.getStack().size()))
      .append("# Circular reference to '")
      .append(getSimpleName())
      .append("' detected! Please re-visit the schema and remove the reference.\n");

    trace(context, "<- [circular-ref:select]", String.format("-> out: %s", getSimpleName()));
//    context.leave(this);
  }

  private String getParentName() {
    Type type = this;
    do {
      type = type.getParent();
      if (type instanceof Obj || type instanceof Union || type instanceof Composed) {
        final String name = type.getName();
        if (name.startsWith("[anonymous:")) {
          continue;
        }

        return name;
      }

    } while (type != null);

    return getParent().getSimpleName();
  }
}
