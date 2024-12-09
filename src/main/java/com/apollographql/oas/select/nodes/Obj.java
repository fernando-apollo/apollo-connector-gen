package com.apollographql.oas.select.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.props.Prop;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static com.apollographql.oas.select.log.Trace.*;

public class Obj extends Type {

  private final Schema schema;
  private boolean visited;

  public Obj(final Type parent, final String name, final Schema schema) {
    super(parent, name);
    this.schema = schema;
  }

  public Schema getSchema() {
    return schema;
  }

  @Override
  public String id() {
    return "obj://" + (getName() != null ? getName() : "[anonymous:" + hashCode() + "] <- " + getParent().getName());
  }

  @Override
  public void add(final Type child) {
    if (getChildren().contains(child)) {
      throw new IllegalArgumentException("Should not be adding this twice! in " + getName());
    }
    super.add(child);
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [obj]", "in " + getName());


    // we don't store Anonymous objects
    if (!inComposed(this)) {
      visitProperties(context);
      context.store(getName(), this);
    }
    else {
      collectProperties(context, true);
    }

    trace(context, "<- [obj]", "out " + getName());
    context.leave(this);
  }

  public static boolean inComposed(Type current) {
    Type parent = current;
    do {
      parent = parent.getParent();
    }
    while (parent != null && !(parent instanceof Composed));

    if (parent != null) {
      trace(null, " [obj::in-composed]", "in composed " + parent.id());
    }

    return parent != null;
  }

  @Override
  public Set<Type> dependencies() {
    final Set<Type> set = new HashSet<>();

    for (Type p : getProps().values()) {
      set.addAll(p.dependencies());
    }

    return set;
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    if (getProps().isEmpty()) {
      return;
    }

    context.enter(this);
    trace(context, "-> [obj::generate]", String.format("-> in: %s", this.getName()));

    writer.append("type ")
      .append(NameUtils.getRefName(getName()))
      .append(" {\n");

    for (Prop prop : this.getProps().values()) {
      trace(context, "-> [obj::generate]", String.format("-> property: %s (parent: %s)", prop.getName(), prop.getParent().getSimpleName()));
      prop.generate(context, writer);
    }

    writer.append("}\n\n");

    trace(context, "<- [obj::generate]", String.format("-> out: %s", this.getName()));
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    if (context.getStack().contains(this)) {
      warn(context, "[obj::select]", "Possible recursion! Stack should not already contain " + this);
      return;
    }
    context.enter(this);
    trace(context, "-> [ref::select]", String.format("-> in: %s", this.getSimpleName()));

    for (Prop prop : this.getProps().values()) {
      prop.select(context, writer);
    }

    trace(context, "<- [ref::select]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  @Override
  public String toString() {
    return "Obj {" +
      "name='" + name + '\'' +
      ", children=" + children.size() +
      ", props=" + props.size() +
      '}';
  }

  private void visitProperties(final Context context) {
    final Map<String, Schema> properties = schema.getProperties();
    trace(context, "-> [obj::props]", "in props " + (properties.isEmpty() ? "0" : properties.size()));

    if (properties.isEmpty()) {
      trace(context, "<- [obj::props]", "no props " + getProps().size());
      return;
    }

    final Set<Map.Entry<String, Schema>> sorted = properties.entrySet()
      .stream()
      .sorted((o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey()))
      .collect(Collectors.toCollection(LinkedHashSet::new));

    final List<String> collected = sorted.stream()
      .map(e -> Factory.fromProperty(this, e.getKey(), e.getValue()))
      .map((Prop p) -> p.forPrompt(context))
      .collect(Collectors.toList());

    final String propertiesNames = String.join(",\n - ", collected);
    String owner = getSimpleName();
    if (owner == null && getParent() instanceof Composed) {
      owner = getParent().getSimpleName();
    }

    final boolean addAll = Prompt.get().prompt("Add all properties from " + owner + "?: \n - " + propertiesNames + "\n");


    /* we should only prompt for properties if:
     * 1. we are NOT a comp://all-of
     * 2. the comp://all-of contains our name (i.e: #/component/schemas/Extensible
     */

    for (final Map.Entry<String, Schema> entry : sorted) {
      final String propertyName = entry.getKey();
      final Schema propertySchema = entry.getValue();

      final Prop prop = Factory.fromProperty(this, propertyName, propertySchema);

      if (addAll || Prompt.get().prompt(indent(context) + "add property '" + prop.forPrompt(context) + "'?")) {
        trace(context, "   [obj::props]", "prop: " + prop);

        // add property to our dependencies
        getProps().put(propertyName, prop);

        if (!this.getChildren().contains(prop)) {
          this.add(prop);
        }
      }
    }

    for (final Prop prop : getProps().values()) {
      prop.visit(context);
    }

    trace(context, "<- [obj::props]", "out props " + getProps().size());
  }

  private void collectProperties(final Context context, boolean visitProperties) {
    final Map<String, Schema> properties = schema.getProperties();
    trace(context, "-> [obj::props]", "in props " + (properties.isEmpty() ? "0" : properties.size()));

    if (properties.isEmpty()) {
      trace(context, "<- [obj::props]", "no props " + getProps().size());
      return;
    }

    properties.entrySet().stream()
      .map(e -> Factory.fromProperty(this, e.getKey(), e.getValue()))
      .forEach(prop -> {
        getProps().put(prop.getName(), prop);
        if (!getChildren().contains(prop)) {
          add(prop);
        }
      });

    if (visitProperties) {
      for (final Prop prop : getProps().values()) {
        prop.visit(context);
      }
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    final Obj obj = (Obj) o;
    return Objects.equals(schema, obj.schema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), schema);
  }
}
