package com.apollographql.oas.select.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.nodes.props.Prop;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static com.apollographql.oas.select.log.Trace.trace;
import static com.apollographql.oas.select.log.Trace.warn;

public abstract class Type {
  protected String name;
  protected List<Type> children = new LinkedList<>();
  private final Type parent;

  protected Map<String, Prop> props = new LinkedHashMap<>();

  protected boolean visited;

  public Type(final Type parent, final String name) {
    this.parent = parent;
    this.name = name;
  }

  public String id() {
    return getName();
  }

  public abstract void visit(Context context);

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public boolean isVisited() {
    return visited;
  }

  public void setVisited(final boolean visited) {
    this.visited = visited;
  }

  public void add(Type child) {
    if (getChildren().contains(child)) {
      warn(null, "[type]", "Should not be adding this twice! in " + id() + ", trying to add " + child.id() + " with children: " + getChildren());
      return;
    }

    this.children.add(child);
  }

  public Type getParent() {
    return parent;
  }

  public Map<String, Prop> getProps() {
    return props;
  }

  public List<Type> getChildren() {
    return children;
  }

  public void generate(Context context, Writer writer) throws IOException {
    throw new IllegalStateException("Not yet implemented for " + getClass().getSimpleName());
  }

  public String getSimpleName() {
    return NameUtils.getRefName(getName());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " {" +
      "name='" + name + '\'' +
      ", children=" + children.size() +
      ", props=" + props.size() +
      '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Type type = (Type) o;
    return Objects.equals(name, type.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public void select(final Context context, final Writer writer) throws IOException {
    throw new IllegalStateException("Not yet implemented for " + getClass().getSimpleName());
  }

  public String forPrompt(final Context context) {
    return getSimpleName() + " (" + getClass().getSimpleName() + ")";
  }

  public Set<Type> dependencies(final Context context) {
    if (!isVisited()) {
      visit(context);
    }

    final Set<Type> set = new HashSet<>(getChildren());

    // by default dependencies will be children, except in objects and composed types
    for (Type t : getChildren()) {
      set.addAll(t.dependencies(context));
    }

    return set;
  }

  public static String getRootPathFor(Type type) {
    StringBuilder builder = new StringBuilder();
    Type current = type;
    int indent = 0;
    do {
      builder.append(" <- ")
        .append(" ".repeat(indent++))
        .append(current.id())
        .append(" (")
        .append(current.getClass().getSimpleName())
        .append(")\n");
    }
    while ((current = current.getParent()) != null);

    return builder.toString();
  }

  protected String getOwner() {
    String owner = getSimpleName();

    if (owner == null && getParent() instanceof Composed) {
      owner = getParent().getSimpleName();
    }

    return owner;
  }
}
