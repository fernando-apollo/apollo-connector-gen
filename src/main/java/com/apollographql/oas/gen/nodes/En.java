package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.nodes.params.Param;
import com.apollographql.oas.gen.nodes.props.Prop;
import io.swagger.v3.oas.models.media.Schema;
import joptsimple.internal.Strings;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.apollographql.oas.gen.log.Trace.trace;

public class En extends Type {
  private final Schema<?> schema;
  private final List<String> items;

  public En(final Type parent, final Schema<?> schema, final List<String> items) {
    super(parent, "enum");
    this.schema = schema;
    this.items = items;
  }


  public Schema<?> getSchema() {
    return schema;
  }

  public List<String> getItems() {
    return items;
  }

  @Override
  public String id() {
    return "enum:" + getItems();
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [enum]", "in: " + getItems());

    setVisited(true);

    trace(context, "<- [enum]", "out: " + getItems());
    context.leave();
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [enum::generate]", String.format("-> in: %s", this.getSimpleName()));

    if (!context.inContextOf(Param.class, this)) {
      String builder = "enum " +
        getSimpleName() +
        " {\n" +
        Strings.join(items.stream().map(s -> " " + s).toList(), ",\n") +
        "\n}\n\n";

      writer.write(builder);
    }

    trace(context, "<- [enum::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave();
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [ref::select]", String.format("-> in: %s", this.getSimpleName()));

    Set<Type> dependencies = dependencies(context);

    for (Type dependency : dependencies) {
      dependency.select(context, writer);
    }

    trace(context, "<- [ref::select]", String.format("-> out: %s", this.getSimpleName()));
    context.leave();
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    return Collections.emptySet();
  }

  @Override
  public Map<String, Prop> getProps() {
    return Collections.emptyMap();
  }

  @Override
  public String toString() {
    return "Enum {" +
      "name='" + name + '\'' +
      ", values=" + getItems().size() +
      '}';
  }
}
