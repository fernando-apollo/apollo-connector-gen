package com.apollographql.oas.select.nodes;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.params.Param;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static com.apollographql.oas.select.log.Trace.trace;
import static com.apollographql.oas.select.log.Trace.warn;

public class GetOp extends Type {
  private final Operation get;

  private String originalPath;
  private String summary;

  private List<Param> parameters = new LinkedList<>();

  private Type resultType;

  public GetOp(final String name, final Operation get) {
    super(null, name);
    this.get = get;
  }

  public String getOriginalPath() {
    return originalPath;
  }

  public void setOriginalPath(final String originalPath) {
    this.originalPath = originalPath;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(final String summary) {
    this.summary = summary;
  }

  public Type getResultType() {
    return resultType;
  }

  public List<Param> getParameters() {
    return parameters;
  }

  public Operation getGet() {
    return get;
  }

  @Override
  public String id() {
    return "get://" + getName();
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
    trace(context, "-> [get]", "in " + getName());

    // 1. visit parameters
    visitParameters(context);

    // 2. visit responses
    visitResponses(context);

    trace(context, "<- [get]", "out " + getName());
    context.leave(this);
  }

  @Override
  public String toString() {
    return "Get {" +
      "name='" + name + '\'' +
      '}';
  }

  private void visitParameters(final Context context) {
    trace(context, "-> [get::parameters]", "in: " + getName());

    if (this.get.getParameters() != null && !this.get.getParameters().isEmpty()) {
      this.parameters = this.get.getParameters().stream().map((Parameter p) -> visitParameter(context, this, p)).toList();
    }
    else {
      this.parameters = Collections.emptyList();
    }

    trace(context, "<- [get::parameters]", "out: " + getName());
  }

  private void visitResponses(final Context context) {
    trace(context, "-> [get::responses]", "in " + getName());

    final List<Map.Entry<String, ApiResponse>> filtered = this.get.getResponses().entrySet().stream()
      .filter(e -> e.getKey().equals("200")) // || e.getKey().equals("default"))
      .toList();

    for (Map.Entry<String, ApiResponse> e : filtered) {
      visitResponse(context, e.getKey(), e.getValue());
    }

    trace(context, "<- [get::responses]", "out " + getName());
  }

  private void visitResponse(final Context context, String code, final ApiResponse response) {
    if (response.get$ref() != null) {
      visitResponseRef(context, response);
    }
    else if (response.getContent() != null) {
      final Optional<Map.Entry<String, MediaType>> first = findJsonContent(response.getContent());
      if (first.isEmpty()) {
        warn(context, "  [" + code + "]", "no mediaType found for content application/json, bailing out!");
      }
      else {
        visitResponseContent(context, code, response);
      }
    }
    else {
      throw new IllegalStateException("Not yet implemented for: " + response);
    }
  }

  private void visitResponseContent(final Context context, final String code, final ApiResponse response) {
    trace(context, "-> [get::responses::content]", "in " + getName());

    final Content content = response.getContent();
    final MediaType mediaType = findJsonContent(content).get().getValue();

    this.resultType = Factory.fromSchema(context, this, mediaType.getSchema());
    this.resultType.visit(context);

    trace(context, "<- [get::responses::content]", "out " + getName());
  }

  private static Optional<Map.Entry<String, MediaType>> findJsonContent(final Content content) {
    Optional<Map.Entry<String, MediaType>> first = content.entrySet().stream()
      .filter(entry -> entry.getKey().contains("application/json"))
      .findFirst();

    return first;
  }

  private void visitResponseRef(final Context context, final ApiResponse response) {
    trace(context, "-> [get::responses::ref]", "in: " + getName() + ", ref: " + response.get$ref());

    final ApiResponse lookup = context.lookupResponse(response.get$ref());
    visitResponse(context, response.get$ref(), lookup);

//    final Type responseType = Factory.fromResponse(context, this, response);
//    assert responseType != null;
//    responseType.visit(context);

    trace(context, "<- [get::responses::ref]", "out: " + getName());
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    context.enter(this);
    trace(context, "-> [get::generate]", String.format("-> in: %s", this.getName()));

    if (getSummary() != null || getOriginalPath() != null) {
      writer.append("  \"\"\"\n").append("  ");

      if (getSummary() != null) {
        writer.append(getSummary()).append(" ");
      }

      // document the original path
      if (getOriginalPath() != null) {
        writer.append("(").append(getOriginalPath()).append(")");
      }

      writer.append("\n  \"\"\"\n");
    }

    writer.append("  ").append(NameUtils.genOperationName(getOriginalPath(), getGet()));

    // TODO: gen parameters
    generateParameters(context, writer);

    if (getResultType() != null) {
      writer.append(": ");
      getResultType().generate(context, writer);
    }

    writer.append("\n");
//    writer.write(writer.toString());

    trace(context, "<- [get::generate]", String.format("-> out: %s", this.getName()));
    context.leave(this);
  }

  private void generateParameters(Context context, Writer writer) throws IOException {
    final List<? extends Type> parameters = getParameters();

    if (parameters.isEmpty()) return;

    writer.append("(");

    for (int i = 0; i < parameters.size(); i++) {
      if (i > 0) writer.append(", ");

      final Type parameter = parameters.get(i);
      parameter.generate(context, writer);
    }

    writer.append(")");
  }

  private Param visitParameter(final Context context, final Type parent, final Parameter p) {
    trace(context, "->[visitParameter]", "begin: " + p.getName());

    final Param param = Factory.fromParam(context, parent, p);
    assert param != null;

    param.visit(context);

    trace(context, "<-[visitParameter]", "end: " + p.getName());
    return param;
  }
}
