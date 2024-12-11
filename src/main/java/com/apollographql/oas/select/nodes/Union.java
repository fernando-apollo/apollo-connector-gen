package com.apollographql.oas.select.nodes;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.params.Param;
import com.apollographql.oas.select.nodes.props.Prop;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static com.apollographql.oas.select.log.Trace.print;
import static com.apollographql.oas.select.log.Trace.trace;

public class Union extends Type {
  private final List<Schema> schemas;

  public Union(final Type parent, final String name, final List<Schema> schemas) {
    super(parent, name);
    this.schemas = schemas;
  }

  @Override
  public String id() {
    return "union://" + getName();
  }

  public List<Schema> getSchemas() {
    return schemas;
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [union]", "in: " + getSchemas().stream().map(Schema::get$ref).toList());

    if (!context.inContextOf(Composed.class, this))
      print(null, "In union: " + getOwner());

    final Map<String, Prop> collected = new LinkedHashMap<>();

    for (final Schema<?> refSchema : getSchemas()) {
      final Type type = Factory.fromSchema(this, refSchema);
      trace(context, "union", "of type: " + type);

      type.visit(context);
      collected.putAll(type.getProps());
    }

    if (!context.inContextOf(Param.class, this))
      visitProperties(context, collected);

    // store the union for generation
    if (getName() != null)
      context.store(getName(), this);

    setVisited(true);

    trace(context, "<- [union]", "out: " + getSchemas().stream().map(Schema::get$ref).toList());
    context.leave();
  }

  private void visitProperties(final Context context, final Map<String, Prop> collected) {
    final String propertiesNames = String.join(",\n - ",
      collected.values().stream().map(Type::getName).toList());

    final char addAll = Prompt
      .get()
      .yesNoSelect(" -> Add all properties from " + getName() + "?: \n - " + propertiesNames + "\n");

    if ((addAll != 'y' && addAll != 's')) {
      return;
    }

    for (final Map.Entry<String, Prop> entry : collected.entrySet()) {
      final Prop prop = entry.getValue();
      if (addAll == 'y' || Prompt.get().yesNo("Add field '" + prop.forPrompt(context) + "'?")) {
        trace(context, "   [union]", "prop: " + prop);

        // add property to our dependencies
        getProps().put(prop.getName(), prop);

        if (!this.getChildren().contains(prop)) {
          this.add(prop);
        }
      }
    }
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [union::generate]", "in: " + getSchemas().stream().map(Schema::get$ref).toList());

    if (context.inContextOf(Param.class, this)) {
      for (Type child : getChildren()) {
        child.generate(context, writer);
      }
    }
    else {
      // when we generate this Union in GQL it will be something like
      // union MyUnion = Type1 | Type2 | Type3 -> name + "=" + types.join(' | ')
      // and then we pray that the types are defined somewhere else
      writer.append("#### NOT SUPPORTED YET BY CONNECTORS!!! union ").append(getSimpleName()).append(" = ");
      writer.append(String.join("# | ", getChildren().stream().map(Type::getName).toList()));
      writer.append("#\n\n");

      trace(context, "   [union::generate]", String.format("[union] -> object: %s", this.getName()));

      writer.append("type ")
        .append(getSimpleName())
        .append(" { #### replacement for Union ")
        .append(getSimpleName())
        .append("\n");

      for (Prop prop : this.getProps().values()) {
        trace(context, "-> [union::generate]", String.format("-> property: %s (parent: %s)", prop.getName(), prop.getParent().getSimpleName()));
        prop.generate(context, writer);
      }

      writer.append("} ### End replacement for ")
        .append(getSimpleName())
        .append("\n\n");
    }

    trace(context, "<- [union::generate]", "out: " + getSchemas().stream().map(Schema::get$ref).toList());
    context.leave();
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    return Collections.emptySet();
  }

  @Override
  public void select(final Context context, Writer writer) throws IOException {
    for (Prop prop : this.getProps().values()) {
      prop.select(context, writer);
    }
  }
}
