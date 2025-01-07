package com.apollographql.oas.gen.nodes;

import com.apollographql.oas.gen.context.Context;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.gen.log.Trace.trace;

public class Response extends Type {
  private final Schema mediaSchema;

  public Schema getMediaSchema() {
    return mediaSchema;
  }

  private Type responseType;

  public void setResponseType(final Type type) {
    this.responseType = type;
  }

  public Type getResponseType() {
    return responseType;
  }


  public Response(final Type parent, final String name, final Schema mediaSchema, final Type response) {
    super(parent, name);
    this.mediaSchema = mediaSchema;
    this.responseType = response;
  }

  @Override
  public String id() {
    return "res:" + getName();
  }

  @Override
  public void visit(final Context context) {
    if (!context.enter(this) || isVisited()) return;
    trace(context, "-> [response:visit]", "in " + getName());

    getResponseType().visit(context);
    setVisited(true);

    trace(context, "<- [response:visit]", "out " + getName());
    context.leave(this);
  }

  @Override
  public void generate(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [response:generate]", String.format("-> in: %s", getParent().getName()));

    getResponseType().generate(context, writer);

    trace(context, "<- [response:generate]", String.format("-> out: %s", getParent().getName()));
    context.leave(this);
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [response:select]", String.format("-> in: %s", getParent().getName()));

    getResponseType().select(context, writer);

    trace(context, "<- [response:select]", String.format("-> out: %s", getParent().getName()));
    context.leave(this);
  }
}
