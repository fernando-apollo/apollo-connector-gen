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

  private void writeSchema(final Writer writer) throws IOException {
    final Set<String> generatedSet = context.getGeneratedSet();
    generatedSet.clear();

    writeDirectives(writer);

    final RefCounter counter = new RefCounter(getContext());
    counter.addAll(collected);

    // we can sort by ref count I guess if we wanted to
//    final Map<String, Type> types = context.getTypes();
//    final Set<Type> filtered = types.values().stream()
//      .filter(t -> refs.containsKey(t.getName()))
//      .sorted((o1, o2) -> {
//        int o1rc = refs.get(o1.getName());
//        int o2rc = refs.get(o2.getName());
//        return o1rc > o2rc ? -1 : o1rc < o2rc ? 1 : 0;
//      }).collect(Collectors.toCollection(LinkedHashSet::new));
//
//    for (Type type : filtered) {
//      type.generate(context, writer);
//      generatedSet.add(type.getName());
//    }

    // 1. generated collected types
    for (final Type type : context.getTypes().values()) {
      if (counter.getCount().containsKey(type.getName())) {
        type.generate(context, writer);
        generatedSet.add(type.getName());
      }
    }

    // 2. now operations
    writeQuery(context, writer, collected);
    writer.flush();
  }

  private void writeQuery(final Context context, final Writer writer, final Set<Type> collected)
    throws IOException {

    writer.write("type Query {\n");

    for (final Type type : collected) {
      type.generate(context, writer);

      writeConnector(context, writer, type);
      context.getGeneratedSet().add(type.getName());
    }

    writer.write("}\n\n");
  }

  private void writeConnector(final Context context, final Writer writer, final Type type) throws IOException {
    int indent = 0;

    // we can safely cast to GetOp
    GetOp get = (GetOp) type;

    var spacing = " ".repeat(indent + 4);
    writer.append(spacing).append("@connect(\n");

    var newPath = get.getOriginalPath().replaceAll("\\{([a-zA-Z0-9]+)\\}", "{\\$args.$1}");
    spacing = " ".repeat(indent + 6);
    writer
      .append(spacing).append("source: \"api\"\n")
      .append(spacing).append("http: { GET: \"").append(newPath).append("\" }\n")
      .append(spacing).append("selection: \"\"\"\n");

//      writer.append("#### selection goes here\n");
    if (get.getResultType() != null)
      writeSelection(context, writer, get.getResultType());

    writer.append(spacing).append("\"\"\"\n");

    spacing = " ".repeat(indent + 4);
    writer.append(spacing).append(")\n");
  }

  private void writeSelection(final Context context, final Writer writer, final Type type)
    throws IOException {
    type.select(context, writer);
  }

  private void writeDirectives(Writer writer) throws IOException {
    writer.append("extend schema\n")
      .append("  @link(url: \"https://specs.apollo.dev/federation/v2.10\", import: [\"@key\"])\n")
      .append("  @link(\n")
      .append("    url: \"https://specs.apollo.dev/connect/v0.1\"\n")
      .append("    import: [\"@connect\", \"@source\"]\n")
      .append("  )\n")
      .append("  @source(name: \"api\", http: { baseURL: \"http://localhost:4010\" })\n\n");
  }

}
