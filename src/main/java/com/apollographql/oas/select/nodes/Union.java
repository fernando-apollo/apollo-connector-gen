package com.apollographql.oas.select.nodes;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import io.swagger.v3.oas.models.media.Schema;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.apollographql.oas.select.log.Trace.trace;

public class Union extends Type {
  //  private final Set<String> refs = new LinkedHashSet<>();
  private final List<Schema> schemas;

  public Union(final Type parent, final String name, final List<Schema> schemas) {
    super(parent, name);
    this.schemas = schemas;
  }

//  public Set<String> getRefs() {
//    return refs;
//  }

//  public void addRef(String name) {
//    refs.add(name);
//  }

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

    trace(context, "<- [union]", "out: " + getSchemas().stream().map(Schema::get$ref).toList());
    context.leave(this);
  }
}
