package com.apollographql.oas.select.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.props.Prop;
import com.apollographql.oas.select.nodes.props.PropRef;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static com.apollographql.oas.select.log.Trace.*;

public class Composed extends Type {
  private final Schema schema;

  public Composed(final Type parent, final String name, final Schema schema) {
    super(parent, name);
    this.schema = schema;
  }

  public Schema getSchema() {
    return schema;
  }

  @Override
  public String id() {
    final Schema schema = getSchema();
    final String refs = getChildren().stream().map(Type::id).collect(Collectors.joining(" + "));

    if (schema.getAllOf() != null) {
      return "comp:all-of://" + refs;
    }
    else if (schema.getOneOf() != null) {
      return "comp:one-of://" + refs;
    }

    return "comp://" + refs;
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
      warn(context, "[comp::select]", "Possible recursion! Stack should not already contain " + this);
      return;
    }
    context.enter(this);
    trace(context, "-> [comp::select]", String.format("-> in: %s", this.getSimpleName()));

    final Schema schema = getSchema();

    if (schema.getAllOf() != null) {
      for (Prop prop : getProps().values()) {
        prop.select(context, writer);
      }
    }
    else if (schema.getOneOf() != null) {
      assert getChildren().size() == 1;
      getChildren().get(0).select(context, writer);
    }

    trace(context, "<- [comp::select]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [composed]", "in: " + (getName() == null ? "[object]" : getName()));

    if (!context.inComposeContext(this))
      print(null, "In composed schema: " + getName());

    final ComposedSchema schema = (ComposedSchema) getSchema();
    if (schema.getAllOf() != null) {
      // this translates to a type with all the properties of the allOf schemas
      visitAllOfNode(context, schema);
    }
    else if (schema.getOneOf() != null) {
      // this translates to a Union type
      visitOneOfNode(context, schema);
    }
    else {
      throw new IllegalStateException("Composed.visit: unsupported composed schema: " + schema.getClass().getSimpleName());
    }

    trace(context, "<- [composed]", "out: " + getName());
    context.leave(this);

//    // now do dependencies
//    final List<Prop> dependencies = getProps().values().stream()
//      .filter(p -> p instanceof PropRef)
//      .toList();
//
//    for (final Prop dependency : dependencies) {
//      trace(context, "-> [composed]", "prop dependency: " + dependency.getName());
////      dependency.visit(context);
//      context.addPending(dependency);
//    }
  }

  /* we are collecting all nodes to combine them into a single object -- therefore we must 'silence' the prompt for
   * now until all types are collected and we can retrieve all the properties.  */
  private void visitAllOfNode(final Context context, final ComposedSchema schema) {
    final List<Schema> allOfs = schema.getAllOf();
    final List<String> refs = allOfs.stream().map(Schema::get$ref).toList();

    trace(context, "-> [composed::all-of]", "in: " + String.format("'%s' of: %d - refs: %s", name, allOfs.size(), refs));

    final Map<String, Prop> collected = new LinkedHashMap<>();
    for (int i = 0; i < allOfs.size(); i++) {
      final Schema allOfItemSchema = allOfs.get(i);

      final Type type = Factory.fromSchema(context, this, allOfItemSchema);
      assert type != null;

      // we are visiting all the tree -- then we'll let them choose which properties they want to add
      type.visit(context);

      collected.putAll(type.getProps());
      trace(context, "   [composed::all-of]", "allOf type: " + type);
    }

    final boolean inCompose = context.inComposeContext(this);
    if (inCompose) {
      getProps().putAll(collected);
    }
    else {

      final String propertiesNames = String.join(",\n - ",
        collected.values().stream().map(Type::getName).toList());

      final char addAll = Prompt
        .get()
        .yesNoSelect(" -> Add all properties from " + getName() + "?: \n - " + propertiesNames + "\n");

      /* we should only prompt for properties if:
       * 1. we are NOT a comp://all-of
       * 2. the comp://all-of contains our name (i.e: #/component/schemas/Extensible
       */
      if ((addAll == 'y' || addAll == 's')) {
        for (final Map.Entry<String, Prop> entry : collected.entrySet()) {
          final Prop prop = entry.getValue();
          if (addAll == 'y' || Prompt.get().yesNo("Add field '" + prop.forPrompt(context) + "'?")) {
            trace(context, "   [obj::props]", "prop: " + prop);

            // add property to our dependencies
            getProps().put(prop.getName(), prop);

            if (!this.getChildren().contains(prop)) {
              this.add(prop);
            }
          }
        }
      }
    }

    // we'll store it first, it might avoid recursion
    trace(context, "-> [composed]", "storing: " + getName() + " with: " + this);
    context.store(getName(), this);

    trace(context, "<- [composed::all-of]", "out: " + String.format("'%s' of: %d - refs: %s", name, allOfs.size(), refs));
  }

  private Type visitOneOfNode(final Context context, final ComposedSchema schema) {
    final var oneOfs = schema.getOneOf();
    trace(context, "-> [composed::one-of]", "in: " + String.format("OneOf %s with size: %d", name, oneOfs.size()));

    final Type result = Factory.fromUnion(context, this, oneOfs);
    assert result != null;
    result.visit(context);

    trace(context, "-> [composed::one-of]", "storing: " + getName() + " with: " + this);
    context.store(getName(), this);

    trace(context, "<- [composed::one-of]", "out: " + String.format("OneOf %s with size: %d", name, oneOfs.size()));
    return result;
  }

  static Map<String, Prop> promptPropertySelection(final Context context, final Type root, final Map<String, Prop> properties) {
    if (properties.isEmpty()) {
      trace(context, "<- [obj::props]", "no props found");
      return properties;
    }

    final Set<Map.Entry<String, Prop>> sorted = properties.entrySet()
      .stream()
      .sorted((o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey()))
      .collect(Collectors.toCollection(LinkedHashSet::new));

    final List<String> collected = sorted.stream()
      .map(Map.Entry::getValue)
      .map((Prop p) -> p.forPrompt(context))
      .collect(Collectors.toList());

    final String propertiesNames = String.join(",\n - ", collected);
    String owner = root.getSimpleName();
    if (owner == null && root.getParent() instanceof Composed) {
      owner = root.getParent().getSimpleName();
    }

    final boolean addAll = Prompt.get().yesNo("Add all properties from " + owner + "?: \n - " + propertiesNames + "\n");
    if (addAll) {
      return properties;
    }

    final Map<String, Prop> result = new LinkedHashMap<>();
    for (final Map.Entry<String, Prop> entry : sorted) {
      final Prop prop = entry.getValue();

      if (Prompt.get().yesNo(indent(context) + "add property '" + prop.forPrompt(context) + "'?")) {
        trace(context, "   [obj::props]", "prop: " + prop);

        // add property to our dependencies
        result.put(entry.getKey(), prop);

        if (!result.containsKey(entry.getKey())) {
          result.put(entry.getKey(), entry.getValue());
        }
      }
    }

    return result;
  }
}
