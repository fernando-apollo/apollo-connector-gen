package com.apollographql.oas.select.nodes;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.props.Prop;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    for (final Schema refSchema : getSchemas()) {
      final Type type = Factory.fromSchema(context, this, refSchema);
      assert type != null;
      type.visit(context);
    }

    // store the union for generation
    context.store(getName(), this);

    trace(context, "<- [union]", "out: " + getSchemas().stream().map(Schema::get$ref).toList());
    context.leave(this);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    trace(context, "-> [union::generate]", "in: " + getSchemas().stream().map(Schema::get$ref).toList());

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

    final Set<Type> generatedSet = new LinkedHashSet<>();

    for (Type type : getChildren()) {
      for (Prop prop : type.getProps().values().stream().filter(p -> !generatedSet.contains(p)).toList()) {
        trace(context, "   [union::generate]", String.format("[union] \t -> property: %s (parent: %s)", prop.getName(), prop.getParent()));
        prop.generate(context, writer);

        generatedSet.add(prop);
      }
    }

    writer.append("} ### End replacement for ")
      .append(getSimpleName())
      .append("\n\n");
//    writer.write(writer.toString());

    trace(context, "<- [union::generate]", "out: " + getSchemas().stream().map(Schema::get$ref).toList());
  }

//  @Override TODO:
//  public Set<CType> getSkipSet(com.apollographql.oas.converter.context.Context context) {
//    return getDependencies(context);
//  }

  @Override
  public void select(final Context context, Writer writer) throws IOException {
    for (Type dependency : getChildren()) {
      dependency.select(context, writer);
    }
  }
}
