package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.converter.utils.GqlUtils;
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
    return "enum:" + getName();
  }

  @Override
  public void visit(final Context context) {
    if (isVisited()) return;

    context.enter(this);
    trace(context, "-> [enum:visit]", "in: " + getItems());

    // we need to store a type when it's not in the context of a Param, pending
    // to check if a Union is a problem too
    if (!context.inContextOf(Param.class, this)) {
      context.store(getName(), this);
    }

    setVisited(true);

    trace(context, "<- [enum:visit]", "out: " + getItems());
    context.leave(this);
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
    else if (!context.inContextOf(Union.class, this)) {
      // this is a very weird combination of a
      // #union:String + Enum.'me'
      writer.write(GqlUtils.getGQLScalarType(getSchema()));
    }
    // else do nothing

    trace(context, "<- [enum::generate]", String.format("-> out: %s", this.getSimpleName()));
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    trace(context, "-> [enum::select]", String.format("-> in: %s", this.getSimpleName()));

    final Set<Type> dependencies = dependencies(context);

    for (Type dependency : dependencies) {
      dependency.select(context, writer);
    }

    trace(context, "<- [enum::select]", String.format("-> out: %s", this.getSimpleName()));
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    // do nothing
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
