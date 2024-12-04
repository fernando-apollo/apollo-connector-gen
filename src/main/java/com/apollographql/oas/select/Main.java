package com.apollographql.oas.select;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.GetOp;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.apollographql.oas.select.log.Trace.trace;

public class Main {
  public static void main(String[] args) throws IOException {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/tmf-apis";
    final String source = String.format("%s/sample-oas/petstore.yaml", baseURL);

    if (!new File(source).exists()) {
      throw new FileNotFoundException("Source not found: " + source);
    }

    final OpenAPI parser = new OpenAPIV3Parser().read(source, null, options);
    final Context context = new Context(parser);

    final Paths paths = parser.getPaths();

    final Set<Map.Entry<String, PathItem>> filtered = paths.entrySet()
      .stream().filter(entry -> entry.getValue().getGet() != null)
      .collect(Collectors.toSet());

    final Set<Type> collected = new LinkedHashSet<>();

    for (final Map.Entry<String, PathItem> entry : filtered) {
      if (!Prompt.prompt("   visit '" + entry.getKey() + "'?")) {
        trace(context, "   [visitPath]", entry.getKey() + " skipped");
        continue;
      }

      final Type result = visitPath(context, entry.getKey(), entry.getValue());
      collected.add(result);
    }

    writeSchema(context, collected);
  }

  private static void writeSchema(final Context context, final Set<Type> collected) throws IOException {
    final Set<String> generatedSet = context.getGeneratedSet();

    final StringWriter writer = new StringWriter();
    writeDirectives(writer);

    // 1. generated collected types
    for (final Type type : context.getTypes().values()) {
      type.generate(context, writer);
      generatedSet.add(type.getName());
    }

    // 2. now operations
    writeQuery(context, writer, collected);

    System.out.println(writer);
  }

  private static void writeQuery(final Context context, final StringWriter writer, final Set<Type> collected)
    throws IOException {

    writer.write("type Query {\n");

    for (final Type type : collected) {
      type.generate(context, writer);

      writeConnector(context, writer, type);
      context.getGeneratedSet().add(type.getName());
    }

    writer.write("}\n\n");
  }

  private static void writeConnector(final Context context, final StringWriter writer, final Type type) throws IOException {
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

  private static Type visitPath(final Context context, final String name, final PathItem path) throws IOException {
    trace(context, "-> [visitPath]", String.format("[%s] %s", name, path.getGet().getOperationId()));

    final Type type = visitGet(context, name, path.getGet());
    trace(context, "<- [visitPath]", "out name: " + name);

    return type;
  }

  private static Type visitGet(final Context context, final String name, final Operation get) {
    Type operation = Factory.createGetOperation(name, get);
    operation.visit(context);
    return operation;
  }

  private static void writeSelection(final Context context, final StringWriter writer, final Type type)
    throws IOException {
    type.select(context, writer);
  }

  private static void writeDirectives(Writer writer) throws IOException {
    writer.append("extend schema\n")
      .append("  @link(url: \"https://specs.apollo.dev/federation/v2.10\", import: [\"@key\"])\n")
      .append("  @link(\n")
      .append("    url: \"https://specs.apollo.dev/connect/v0.1\"\n")
      .append("    import: [\"@connect\", \"@source\"]\n")
      .append("  )\n")
      .append("  @source(name: \"api\", http: { baseURL: \"http://localhost:4010\" })\n\n");
  }

}

