package com.apollographql.oas.select.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.props.Prop;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.apollographql.oas.select.log.Trace.indent;
import static com.apollographql.oas.select.log.Trace.trace;
import static com.apollographql.oas.select.log.Trace.warn;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

public class Obj extends Type {

  private final Schema schema;

  public Obj(final Type parent, final String name, final Schema schema) {
    super(parent, name);
    this.schema = schema;
  }

  public Schema getSchema() {
    return schema;
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [obj]", "in " + getName());

    visitProperties(context);

    // we don't store Anonymous objects
    if (getName() != null) {
      context.store(getName(), this);
    }

    trace(context, "<- [obj]", "out " + getName());
    context.leave(this);
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
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

    final Set<String> collected = properties.entrySet().stream()
      .map(e -> Factory.fromProperty(this, e.getKey(), e.getValue()))
      .map((Prop p) -> {
//        p.visit(context);
        return p.forPrompt(context);
      })
      .collect(Collectors.toSet());

    final String propertiesNames = String.join(",\n - ", collected);
    String owner = getSimpleName();
    if (owner == null && getParent() instanceof Composed) {
      owner = getParent().getSimpleName();
    }

    final boolean addAll = Prompt.get().prompt("Add all properties from " + owner + "?: \n - " + propertiesNames + "\n");

    for (final Map.Entry<String, Schema> entry : properties.entrySet()) {
      final String propertyName = entry.getKey();
      final Schema propertySchema = entry.getValue();

      final Prop prop = Factory.fromProperty(this, propertyName, propertySchema);

      if (addAll || Prompt.get().prompt(indent(context) + "add property '" + prop + "'?")) {
        trace(context, "   [obj::props]", "prop: " + prop);

        // add property to our dependencies
        getProps().put(propertyName, prop);
      }
    }

    for (final Prop prop : getProps().values()) {
      prop.visit(context);
    }

    trace(context, "<- [obj::props]", "out props " + getProps().size());
  }
}
