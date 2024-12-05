package com.apollographql.oas.select.nodes;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.props.Prop;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;

import static com.apollographql.oas.select.log.Trace.trace;

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
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [composed]", "in: " + (getName() == null ? "[object]" : getName()));

    // we'll store it first, it might avoid recursion
    trace(context, "-> [composed]", "storing: " + getName() + " with: " + this);
    context.store(getName(), this);

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
  }

  /* we are collecting all nodes to combine them into a single object -- therefore we must 'silence' the prompt for
   * now until all types are collected and we can retrieve all the properties.  */
  private void visitAllOfNode(final Context context, final ComposedSchema schema) {
//    Prompt.get().mute(true, this);

    final List<Schema> allOfs = schema.getAllOf();
    final List<String> refs = allOfs.stream().map(Schema::get$ref).toList();

    trace(context, "-> [composed::all-of]", "in: " + String.format("'%s' of: %d - refs: %s", name, allOfs.size(), refs));

    for (int i = 0; i < allOfs.size(); i++) {
      final Schema allOfItemSchema = allOfs.get(i);

      final Type type = Factory.fromSchema(context, this, allOfItemSchema);
      assert type != null;

      // we are visiting all the tree -- then we'll let them choose which properties they want to add
      type.visit(context);

      trace(context, "   [composed::all-of]", "allOf type: " + type);
    }

    // we have collected the children, now we need to combine all properties
    collectProperties(this, getProps());

    // critical: we MUST turn the prompt back on
//    Prompt.get().mute(false, this);

    trace(context, "<- [composed::all-of]", "out: " + String.format("'%s' of: %d - refs: %s", name, allOfs.size(), refs));
  }

  private void visitOneOfNode(final Context context, final ComposedSchema schema) {
    final var oneOfs = schema.getOneOf();
    trace(context, "-> [composed::one-of]", "in: " + String.format("OneOf %s with size: %d", name, oneOfs.size()));

    final Type result = Factory.fromUnion(context, this, oneOfs);
    assert result != null;
    result.visit(context);

    trace(context, "<- [composed::one-of]", "out: " + String.format("OneOf %s with size: %d", name, oneOfs.size()));
  }

  private void collectProperties(final Type node, final Map<String, Prop> props) {
    for (Type child : node.getChildren()) {
      props.putAll(child.getProps());
      collectProperties(child, props);
    }
  }
}