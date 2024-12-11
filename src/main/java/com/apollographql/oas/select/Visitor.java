package com.apollographql.oas.select;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.context.RefCounter;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.GetOp;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.prompt.Input;
import com.apollographql.oas.select.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import static com.apollographql.oas.select.log.Trace.trace;

public class Visitor {
  private final OpenAPI parser;
  private Context context;
  private Set<Type> collected;

  public Visitor(final OpenAPI parser) {
    this.parser = parser;
  }

  public OpenAPI getParser() {
    return parser;
  }

  /**
   * @deprecated
   */
  public static void main(String[] args) throws IOException {
    InputStream configFile = Visitor.class.getClassLoader().getResourceAsStream("logging.properties");

    if (configFile == null) {
      throw new IllegalArgumentException("logging.properties file not found in classpath");
    }

    // Load the configuration
    LogManager.getLogManager().readConfiguration(configFile);

    final Input recorder = Prompt.Factory.recorder();
    Prompt.get(recorder);

    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/poc/services";
    final String source = String.format("%s/js-mva-consumer-info_v1.yaml", baseURL);

    final Visitor visitor = fromFile(source);
    visitor.visit();

    System.out.println("---------------- recorder ----------------------");
    final List<Pair<String, String>> records = ((Prompt.Recorder) recorder).getRecords();
    records.forEach(pair -> System.out.println("\"" + pair.getLeft() + "\" /* " + pair.getRight() + " */"));

    System.out.println("---------------- schema ----------------------");
    final StringWriter writer = new StringWriter();
    visitor.writeSchema(writer);
    System.out.println(writer);
  }

  public static Visitor fromFile(final String source) throws IOException {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    if (!new File(source).exists()) {
      throw new FileNotFoundException("Source not found: " + source);
    }

    final OpenAPI parser = new OpenAPIV3Parser().read(source, null, options);

    if (parser == null) throw new IOException("Could not create OpenAPI parser for source file");

    return new Visitor(parser);
  }

  public Set<Type> getCollected() {
    return collected;
  }

  public void visit() throws IOException {
    final OpenAPI parser = getParser();

    final Context context = getContext();
    final Paths paths = parser.getPaths();

    final List<Map.Entry<String, PathItem>> filtered = paths.entrySet()
      .stream().filter(entry -> entry.getValue().getGet() != null)
      .sorted((o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey()))
      .toList();

    final Set<Type> collected = new LinkedHashSet<>();

    for (final Map.Entry<String, PathItem> entry : filtered) {
      if (!Prompt.get().yesNo("visit '" + entry.getKey() + "'?")) {
        trace(context, "   [visitPath]", entry.getKey() + " skipped");
        continue;
      }

      final Type result = visitPath(context, entry.getKey(), entry.getValue());
      collected.add(result);
    }

    this.collected = collected;
  }

  public Context getContext() {
    if (this.context == null) {
      this.context = new Context(getParser());
    }
    return this.context;
  }

  public void writeSchema(Writer writer) throws IOException {
    final Set<String> generatedSet = context.getGeneratedSet();
    generatedSet.clear();

    writeDirectives(writer);

    final RefCounter counter = new RefCounter(getContext());
    counter.addAll(collected);

    final Map<String, Integer> refs = counter.getCount();
    printRefs(refs);

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

  private static void printRefs(final Map<String, Integer> values) {
    System.out.println("----------- ref count -------------- ");
    values.entrySet()//.stream().filter(e -> e.getKey().startsWith("ref://"))
      .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
  }

}
