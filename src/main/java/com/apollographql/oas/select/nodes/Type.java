package com.apollographql.oas.select.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.nodes.props.Prop;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public abstract class Type {
  protected String name;
  protected List<Type> children = new LinkedList<>();
  private final Type parent;

  protected Map<String, Prop> props = new LinkedHashMap<>();

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

  public void add(Type child) {
    if (getChildren().contains(child)) {
      throw new IllegalArgumentException("Should not be adding this twice! in " + getName());
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
    return "Type {" +
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

//  protected void addProperties(Map<String, Schema> properties) {
//    for (var property : properties.entrySet()) {
//      final String propertyName = property.getKey();
//      final Schema propertySchema = property.getValue();
//
//      final Prop prop = createProp(propertyName, propertySchema);
//      if (prop != null) {
//        getProps().put(propertyName, prop);
//      }
//    }
//  }

}
