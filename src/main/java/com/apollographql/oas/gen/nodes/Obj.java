package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.naming.Naming;
import com.apollographql.oas.gen.nodes.props.Prop;
import com.apollographql.oas.gen.nodes.props.PropArray;
import com.apollographql.oas.gen.nodes.props.PropObj;
import com.apollographql.oas.gen.nodes.props.PropRef;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static com.apollographql.oas.gen.log.Trace.*;

@SuppressWarnings({"ALL", "unchecked"})
public class Obj extends Type {

  private final Schema schema;
  private boolean visited;

  public Obj(final Type parent, final String name, final Schema schema) {
    super(parent, name);
    this.schema = schema;

    // need to update name depending on the parent
    updateName();
  }

  public Schema getSchema() {
    return schema;
  }

  @Override
  public String id() {
    return "obj:" + getName();
  }

  private void updateName() {
    String name = getName();

    // if we are an 'items' inline object, we'll try and create a better name for the
    // items in the array, as it might clash with others
    if (name == null || (name != null && name.equals("items"))) {
      final Type parent = getParent();
      final String parentName = parent.getName();

      if (parent instanceof Ref) {
        name = parentName.replace("ref:", "obj:");
      }
      else if (parent instanceof Array || parent instanceof PropArray) {
        name = StringUtils.capitalize(NameUtils.genParamName(Naming.getRefName(parentName) + "Item"));
      }
      else if (parent instanceof Response) {
        GetOp op = (GetOp) parent.getParent();
        // happens when the response is inlined
        name = op.getGqlOpName() + "Response";
      }
      else if (parent instanceof Obj) {
        name = parentName + "Obj";
      }
      else {
        name = "[anonymous:" + hashCode() + "]";
      }
    }

    this.name = name;
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
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [obj:visit]", "in " + getName());

    if (!context.inContextOf(Composed.class, this))
      print(null, "In object: " + (getName() != null ? getName() : getOwner()));

    visitProperties(context);
    setVisited(true);

    // we don't store Anonymous objects
    if (getName() != null)
      context.store(getName(), this);

    trace(context, "<- [obj:visit]", "out " + getName());
    context.leave(this);
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    if (!isVisited()) {
      this.visit(context);
    }

    context.enter(this);
    trace(context, "-> [obj:dependencies]", "in " + getName());

    final Set<Type> set = new HashSet<>();
    for (Type p : getProps().values().stream()
      .filter(p -> p instanceof PropRef || p instanceof PropArray || p instanceof PropObj).toList()) {
      final Set<Type> dependencies = p.dependencies(context);
      set.addAll(dependencies);
    }

    trace(context, "<- [obj:dependencies]", "out " + getName());
    context.leave(this);
    return set;
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    if (getProps().isEmpty()) {
      return;
    }

    if (context.inContextOf(Response.class, this)) {
//      writer.append(NameUtils.getRefName(getName()));
      writer.append(Naming.genTypeName(getName()));
      return;
    }

    context.enter(this);
    trace(context, "-> [obj::generate]", String.format("-> in: %s", this.getName()));

//    final String sanitised = StringUtils.capitalize(NameUtils.genParamName(NameUtils.getRefName(getName())));
    final String sanitised = Naming.genTypeName(getName());
    final String refName = Naming.getRefName(getName());

    writer.append("type ")
      .append(sanitised.equals(refName) ? refName : sanitised)
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
    trace(context, "-> [ref::select]", String.format("-> in: %s", this.getSimpleName()));

    for (Prop prop : this.getProps().values()) {
      prop.select(context, writer);
    }

    trace(context, "<- [ref::select]", String.format("-> out: %s", this.getSimpleName()));
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
    if (schema.getProperties() == null) return;

    //noinspection unchecked
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

    final Map<String, Prop> collected = sorted.stream()
      .map(e -> Factory.fromProperty(context, this, e.getKey(), e.getValue()))
      .collect(Collectors.toMap(Prop::getName, prop -> prop));

    final String propertiesNames = collected.values().stream()
      .map(p -> p.forPrompt(context))
      .collect(Collectors.joining(",\n - "));

    final boolean inCompose = context.inContextOf(Composed.class, this);

    final int inComposeIdx = findAncestorOf(this, Composed.class);
    final int inArrayIdx = findAncestorOf(this, PropArray.class);

    if (!inCompose || (inArrayIdx > inComposeIdx)) {
      System.out.println("Obj.visitProperties HERE");
    }

    trace(context, "   [obj::props]", getSimpleName() + " is within compose context? " + inCompose);

    final char addAll = (!inCompose || (inArrayIdx > inComposeIdx)) ? context.getPrompt()
      .yesNoSelect(path(), " -> Add all properties from [object] " + getOwner() + "?: \n - " + propertiesNames + "\n") : 'y';

    /* we should only prompt for properties if:
     * 1. we are NOT a comp://all-of
     * 2. the comp://all-of contains our name (i.e: #/component/schemas/Extensible
     */
    if ((addAll == 'y' || addAll == 's')) {
      for (final Map.Entry<String, Schema> entry : sorted) {
        final String propertyName = entry.getKey();
        final Schema propertySchema = entry.getValue();

        final Prop prop = Factory.fromProperty(context, this, propertyName, propertySchema);

        if (addAll == 'y' || context.getPrompt().yesNo(prop.path(), "Add field '" + prop.forPrompt(context) + "'?")) {
          trace(context, "   [obj::props]", "prop: " + prop);

          // add property to our dependencies
          getProps().put(propertyName, prop);

          if (!this.getChildren().contains(prop)) {
            this.add(prop);
          }
        }
      }
    }

    // now do dependencies -- this works well for Petstore but not for TMF633
    // in Composed we'll need to filter out which props we don't want added
    // instead of adding them as a dependency
    addDependencies(context);

    trace(context, "<- [obj::props]", "out props " + getProps().size());
  }

  private void addDependencies(final Context context) {
    final boolean inCompose = context.inContextOf(Composed.class, this);

    if (!inCompose) {
      final List<Prop> dependencies = getProps().values().stream()
        .filter(p -> {
          trace(context, "-> [obj]", "visitProperties - NOT inCompose, in " + id());
          return p instanceof PropRef || p instanceof PropArray || p instanceof PropObj;
        })
        .toList();

      for (final Prop dependency : dependencies) {
        trace(context, "-> [obj]", "prop dependency: " + dependency.getName());
        dependency.visit(context);
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
