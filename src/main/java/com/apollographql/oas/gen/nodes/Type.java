package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.nodes.props.Prop;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static com.apollographql.oas.gen.log.Trace.trace;
import static com.apollographql.oas.gen.log.Trace.warn;

public abstract class Type implements Cloneable {
  protected String name;
  protected List<Type> children = new LinkedList<>();
  private final Type parent;

  protected Map<String, Prop> props = new LinkedHashMap<>();

  protected boolean visited;

  public Type(final Type parent, final String name) {
    this.parent = parent;
    this.name = name;
  }

  protected static Type findAncestor(final Type type) {
    final List<Type> ancestors = getAncestors(type.getParent());
    final int indexOf = ancestors.indexOf(type);
    return indexOf > -1 ? (Ref) ancestors.get(indexOf) : null;
  }

  protected static int findAncestorOf(final Type type, final Class clazz) {
    final List<Type> ancestors = getAncestors(type.getParent());

    for (int i = ancestors.size() - 1; i > -1; i--) {
      final Type ancestor = ancestors.get(i);
      if (ancestor.getClass().isAssignableFrom(clazz)) {
        return i;
      }
    }

    return -1;
  }

  public String id() {
    return getName();
  }

  public String path() {
    final List<Type> ancestors = getAncestors(this);
    return ancestors.stream().map(t -> t.id())
      .collect(Collectors.joining(">"))
      .replaceAll("#/components/schemas", "#/c/s")
      ;
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

    if (getAncestors(this).contains(child)) {
      warn(null, "[context]", "Recursion? Ancestors contain this type already: \n" + Type.getRootPathFor(child));
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

    context.enter(this);
    trace(context, "-> [type:dependencies]", String.format("-> in: %s", getSimpleName()));
    final Set<Type> set = new HashSet<>(getChildren());

    // by default dependencies will be children, except in objects and composed types
    for (Type child : getChildren()) {
      if (child instanceof CircularRef) continue;
      /*if (context.isVisiting(child)) {
        continue;
      }*/

      trace(context, "  [type:dependencies]", "checking child " + child.getSimpleName());
      final Set<Type> dependencies = child.dependencies(context);
      trace(context, "  [type:dependencies]", "dependencies = " + dependencies);
      set.addAll(dependencies);
    }

    trace(context, "<- [type:dependencies]", String.format("<- out: %s", getSimpleName()));
    context.leave(this);
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

  public static List<Type> getAncestors(Type type) {
    List<Type> result = new LinkedList<>();
    result.add(type);

    Type parent = type;
    while ((parent = parent.getParent()) != null) {
      result.add(0, parent);
    }

    return result;
  }

  protected String getOwner() {
    String owner = getSimpleName();

    if (owner == null && getParent() instanceof Composed) {
      owner = getParent().getSimpleName();
    }

    return owner;
  }

  public static Type findTypeIn(final String path, final Collection<Type> collection) {

    Type found = null;
    for (Type type : collection) {
      if (type.path().equals(path)) {
        found = type;
        break;
      }
    }

    // search props
    if (found == null) {
      for (Type type : collection) {
        for (Type prop : type.getProps().values()) {
          if (prop.path().equals(path)) {
            found = prop;
            break;
          }
        }
      }
    }

    // search children
    if (found == null) {
      for (Type type : collection) {
        final List<Type> children = type.getChildren();
        found = findTypeIn(path, children);
        if (found != null) {
          break;
        }
        ;
      }
    }

    return found;
  }

  @Override
  public Type clone() {
    try {
      final Type clone = (Type) super.clone();
      clone.setVisited(isVisited());
      clone.setName(getName());
      // TODO: copy mutable state here, so the clone can't change the internals of the original
      return clone;
    }
    catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
