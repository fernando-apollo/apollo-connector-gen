package com.apollographql.oas.gen;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.context.RefCounter;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.GetOp;
import com.apollographql.oas.gen.nodes.Type;
import com.apollographql.oas.gen.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.*;
import java.util.*;

import static com.apollographql.oas.gen.log.Trace.trace;

public class WebGenerator {
  private final OpenAPI parser;
  private Prompt prompt;
  private Context context;
  private final Set<Type> collected = new LinkedHashSet<>();

  public WebGenerator(final OpenAPI parser, final Prompt prompt) {
    this.parser = parser;
    this.prompt = prompt;
  }

  public OpenAPI getParser() {
    return parser;
  }

  public Prompt getPrompt() {
    return prompt;
  }

  public void setPrompt(final Prompt prompt) {
    this.prompt = prompt;
  }

  public static WebGenerator fromFile(final String source, final Prompt prompt) throws IOException {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    if (!new File(source).exists()) {
      throw new FileNotFoundException("Source not found: " + source);
    }

    final OpenAPI parser = new OpenAPIV3Parser().read(source, null, options);

    if (parser == null) throw new IOException("Could not create OpenAPI parser for source file");

    return new WebGenerator(parser, prompt);
  }

  public List<String> listGetPaths() throws IOException {
    final OpenAPI parser = getParser();

    final Context context = getContext();
    final Paths paths = parser.getPaths();

    return paths.entrySet()
      .stream().filter(entry -> entry.getValue().getGet() != null)
      .sorted((o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey()))
//      .map(entry -> visitPath(context, entry.getKey(), entry.getValue()))
      .map(entry -> entry.getKey())
      .toList();
  }

  public Set<Type> getCollected() {
    return collected;
  }

  public Context getContext() {
    if (this.context == null) {
      this.context = new Context(getParser(), getPrompt());
    }
    return this.context;
  }

  private Type visitPath(final Context context, final String name, final PathItem path) {
    trace(context, "-> [visitPath]", String.format("[%s] %s", name, path.getGet().getOperationId()));

    final Type type = visitGet(context, name, path.getGet());
    trace(context, "<- [visitPath]", "out name: " + name);

    return type;
  }

  private Type visitGet(final Context context, final String name, final Operation get) {
    Type operation = Factory.createGetOperation(name, get);
    operation.visit(context);
    return operation;
  }

  public GetOp getPathResult(final String id) throws IOException {
    final String sanitised = id.startsWith("get:") ? id.substring("get:".length()) : id;
    final Optional<Map.Entry<String, PathItem>> found = getParser().getPaths().entrySet()
      .stream().filter(entry -> entry.getKey().equals(sanitised))
      .findFirst();

    if (found.isPresent()) {
      final GetOp result = (GetOp) visitPath(getContext(), found.get().getKey(), found.get().getValue());
      collected.add(result);

      return result;
    }
    else {
      throw new IllegalArgumentException("Path '" + id + "' not found in spec paths");
    }
  }

  public Type find(final String path) {
    final Type type = Type.findTypeIn(path, this.getCollected());
    if (type != null && !type.isVisited()) {
      type.visit(getContext());
    }

    return type;
  }

  public void writeSchema(Writer writer, final Prompt prompt) throws IOException {
    final ConnectorGen gen = new ConnectorGen(getParser(), prompt); // reuse parser
    gen.visit();
    gen.writeSchema(writer);
  }

}
