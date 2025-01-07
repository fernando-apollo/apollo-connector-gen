package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.params.Param;
import com.apollographql.oas.gen.nodes.props.Prop;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static com.apollographql.oas.gen.log.Trace.*;

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
    return "comp:" + getName();
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    if (!isVisited()) {
      this.visit(context);
    }

    context.enter(this);
    trace(context, "-> [comp::dependencies]", String.format("-> in: %s", this.getName()));

    final Set<Type> set = new HashSet<>();
    if (getSchema().getAllOf() != null) {
      for (Type p : getProps().values()) {
        trace(context, "  [comp:all-of::dependencies]", "checking prop " + p.getName());
        final Set<Type> dependencies = p.dependencies(context);
        trace(context, "  [comp:all-of::dependencies]", dependencies.toString());
        set.addAll(dependencies);
      }
    }

    if (getSchema().getOneOf() != null) {
      // by default dependencies will be children, except in objects and composed types
      for (Type t : getChildren()) {
        trace(context, "  [comp:one-of::dependencies]", "checking child " + t.getName());
        final Set<Type> dependencies = t.dependencies(context);
        trace(context, "  [comp:one-of::dependencies]", dependencies.toString());
        set.addAll(dependencies);
      }
    }

    trace(context, "<- [comp::dependencies]", String.format("-> out: %s", this.getName()));
    context.leave(this);
    return set;
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [comp::generate]", String.format("-> in: %s", this.getName()));

    if (getSchema().getOneOf() != null) {
      getChildren().get(0).generate(context, writer);
    }
    else if (getSchema().getAllOf() != null) {
      if (!getProps().isEmpty()) {
        writer.append("type ")
          .append(NameUtils.getRefName(getName()))
          .append(" {\n");

        for (Prop prop : this.getProps().values()) {
          trace(context, "   [comp::generate]", String.format("-> property: %s (parent: %s)", prop.getName(), prop.getParent().getSimpleName()));
          prop.generate(context, writer);
        }

        writer.append("}\n\n");
      }
    }

    trace(context, "<- [comp::generate]", String.format("-> out: %s", this.getName()));
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
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
  }

  @Override
  public void visit(final Context context) {
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [composed:visit]", "in: " + (getName() == null ? "[object]" : getName()));

    if (!context.inContextOf(Composed.class, this) && !context.inContextOf(Param.class, this))
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

    setVisited(true);

    trace(context, "<- [composed:visit]", "out: " + getName());
    context.leave(this);
  }

  /* we are collecting all nodes to combine them into a single object -- therefore we must 'silence' the prompt for
   * now until all types are collected, and we can retrieve all the properties.  */
  private void visitAllOfNode(final Context context, final ComposedSchema schema) {
    final List<Schema> allOfs = schema.getAllOf();
    final List<String> refs = allOfs.stream().map(Schema::get$ref).toList();

    trace(context, "-> [composed::all-of]", "in: " + String.format("'%s' of: %d - refs: %s", name, allOfs.size(), refs));

    final Map<String, Prop> collected = new LinkedHashMap<>();
    for (int i = 0; i < allOfs.size(); i++) {
      final Schema allOfItemSchema = allOfs.get(i);

      final Type type = Factory.fromSchema(this, allOfItemSchema);
      trace(context, "   [composed::all-of]", "allOf type: " + type);
      assert type != null;

      // we are visiting all the tree -- then we'll let them choose which properties they want to add
      type.visit(context);
      collected.putAll(type.getProps());
    }

    final boolean inCompose = context.inContextOf(Composed.class, this);
    if (inCompose) {
      getProps().putAll(collected);
    }
    else {
      promptPropertySelection(context, collected);
    }

    // we'll store it first, it might avoid recursion
    trace(context, "-> [composed]", "storing: " + getName() + " with: " + this);
    context.store(getName(), this);

    trace(context, "<- [composed::all-of]", "out: " + String.format("'%s' of: %d - refs: %s", name, allOfs.size(), refs));
  }

  private void promptPropertySelection(final Context context, final Map<String, Prop> properties) {
    if (properties.isEmpty()) {
      return;
    }

    final String propertiesNames = String.join(",\n - ",
      properties.values().stream().map(Type::getName).toList());

    final char addAll = context.getPrompt()
      .yesNoSelect(path(), " -> Add all properties from [composed] " + getName() + "?: \n - " + propertiesNames + "\n");

    if ((addAll == 'y' || addAll == 's')) {
      for (final Map.Entry<String, Prop> entry : properties.entrySet()) {
        final Prop prop = entry.getValue();
        if (addAll == 'y' || context.getPrompt().yesNo(prop.path(), "Add field '" + prop.forPrompt(context) + "'?")) {
          trace(context, "   [composed::props]", "prop: " + prop);

          // add property to our dependencies
          getProps().put(prop.getName(), prop);

          if (!this.getChildren().contains(prop)) {
            this.add(prop);
          }
        }
      }
    }
  }

  private void visitOneOfNode(final Context context, final ComposedSchema schema) {
    final List<Schema> oneOfs = schema.getOneOf();
    trace(context, "-> [composed::one-of]", "in: " + String.format("OneOf %s with size: %d", name, oneOfs.size()));

    final Type result = Factory.fromUnion(context, this, oneOfs);
    assert result != null;
    result.visit(context);

    trace(context, "-> [composed::one-of]", "storing: " + getName() + " with: " + this);
    if (getName() != null)
      context.store(getName(), this);

    trace(context, "<- [composed::one-of]", "out: " + String.format("OneOf %s with size: %d", name, oneOfs.size()));
  }

}
