package com.apollographql.oas.gen;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.context.RefCounter;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.GetOp;
import com.apollographql.oas.gen.nodes.Type;
import com.apollographql.oas.gen.nodes.params.Param;
import com.apollographql.oas.gen.prompt.Input;
import com.apollographql.oas.gen.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.*;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import static com.apollographql.oas.gen.log.Trace.trace;

public class ConnectorGen {
  private final OpenAPI parser;
  private Prompt prompt;
  private Context context;
  private Set<Type> collected;

  public ConnectorGen(final OpenAPI parser, final Prompt prompt) {
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

  /**
   * @deprecated Use only for recording
   */
  public static void main(String[] args) throws IOException {
    InputStream configFile = ConnectorGen.class.getClassLoader().getResourceAsStream("logging.properties");

    if (configFile == null) {
      throw new IllegalArgumentException("logging.properties file not found in classpath");
    }

    // Load the configuration
    LogManager.getLogManager().readConfiguration(configFile);

    final Input recorder = Prompt.Factory.mapRecorder();
    final Prompt prompt = Prompt.create(recorder);

    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/tmf-apis/tmf-specs";
//    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/tmf-apis/sample-oas";
//    final String source = String.format("%s/TMF717_Customer360-v5.0.0.oas.yaml", baseURL);
    final String source = String.format("%s/TMF637-ProductInventory-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/petstore.yaml", baseURL);

//    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/poc/services";
//    final String baseURL = "/Users/fernando/Downloads";
//    final String source = String.format("%s/js-mva-consumer-info_v1.yaml", baseURL);
//    final String source = String.format("%s/js-mva-homepage-product-selector_v3.yaml", baseURL);
//    final String source = String.format("%s/most-popular-product.yaml", baseURL);

    final ConnectorGen generator = fromFile(source, prompt);
    generator.visit();

    System.out.println("---------------- recorder ----------------------");
    final List<Pair<String, String>> records = ((Prompt.MapRecorder) recorder).getRecords();
    records.forEach(pair -> System.out.println(pair.getLeft() + " -> " + pair.getRight()));

    System.out.println("---------------- schema ----------------------");
    final StringWriter writer = new StringWriter();
    generator.writeSchema(writer);
    System.out.println(writer);
  }

  public static ConnectorGen fromFile(final String source, final Prompt prompt) throws IOException {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    if (!new File(source).exists()) {
      throw new FileNotFoundException("Source not found: " + source);
    }

    final OpenAPI parser = new OpenAPIV3Parser().read(source, null, options);

    if (parser == null) throw new IOException("Could not create OpenAPI parser for source file");

    return new ConnectorGen(parser, prompt);
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
      if (!context.getPrompt().yesNo("get:" + entry.getKey(), "visit '" + entry.getKey() + "'?")) {
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
      this.context = new Context(getParser(), getPrompt());
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

    final String request = buildRequestMethodAndArgs(get);

    spacing = " ".repeat(indent + 6);
    writer
      .append(spacing).append("source: \"api\"\n")
      .append(spacing).append("http: " + request + "\n")
      .append(spacing).append("selection: \"\"\"\n");

    if (get.getResultType() != null)
      writeSelection(context, writer, get.getResultType());

    writer.append(spacing).append("\"\"\"\n");

    spacing = " ".repeat(indent + 4);
    writer.append(spacing).append(")\n");
  }

  private static String buildRequestMethodAndArgs(final GetOp get) {
    StringBuilder builder = new StringBuilder();

    builder
      .append("\"")
      .append(get.getOriginalPath().replaceAll("\\{([a-zA-Z0-9]+)\\}", "{\\$args.$1}"));

    // we can potentially the required params here:
    if (!get.getParameters().isEmpty()) {
      // add the query params
      final List<Parameter> queries = get.getGet().getParameters().stream()
        .filter(p -> p.getRequired() && p.getIn() != null && p.getIn().equalsIgnoreCase("query"))
        .toList();

      if (!queries.isEmpty()) {
        final String queryString = queries.stream()
          .map(p -> p.getName() + "={$args." + NameUtils.genParamName(p.getName()) + "}")
          .collect(Collectors.joining("&"));

        builder
          .append("?")
          .append(queryString);
      }

      // add the headers
      final List<Parameter> headers = get.getGet().getParameters().stream()
        .filter(p -> p.getIn() != null && p.getIn().equalsIgnoreCase("header"))
        .toList();

      builder.append("\"\n");

      if (!headers.isEmpty()) {
        var spacing = " ".repeat(6);
        builder
          .append(spacing)
          .append("headers: [\n");

        spacing = " ".repeat(8);
        for (Parameter p : headers) {
          final String name = p.getName();

          String value = null;
          if (p.getExample() != null)
            value = p.getExample().toString();

          if (p.getExamples() != null && p.getExamples().isEmpty())
            value = String.join(",", p.getExamples().keySet());

          if (value == null)
              value = "<placeholder>";

          builder
            .append(spacing)
            .append("{ ").append("name: \"").append(name).append("\", value: \"").append(value).append("\" }\n");
        }

        spacing = " ".repeat(6);
        builder
          .append(spacing)
          .append("]");
      }
    }
    else {
      builder.append("\"");
    }

    final String request = "{ GET: " + builder + " }";
    return request;
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
    context.setIndent(6);
    type.select(context, writer);
  }

  private void writeDirectives(Writer writer) throws IOException {
    final OpenAPI api = getParser();
    final String host = getServerUrl(api.getServers().stream().findFirst());

    writer.append("extend schema\n")
      .append("  @link(url: \"https://specs.apollo.dev/federation/v2.10\", import: [\"@key\"])\n")
      .append("  @link(\n")
      .append("    url: \"https://specs.apollo.dev/connect/v0.1\"\n")
      .append("    import: [\"@connect\", \"@source\"]\n")
      .append("  )\n")
      .append("  @source(name: \"api\", http: { baseURL: \"")
      .append(host)
      .append("\" })\n\n");
  }

  private static String getServerUrl(final Optional<Server> server) {
    if (server.isEmpty()) return "http://localhost:4010";

    final Server value = server.get();
    String url = value.getUrl();

    if (value.getVariables() != null) {
      for (Map.Entry<String, ServerVariable> variable : value.getVariables().entrySet()) {
        url = url.replace("{" + variable.getKey() + "}", variable.getValue().getDefault());
      }
    }

    return url;
  }

  private static void printRefs(final Map<String, Integer> values) {
    System.out.println("----------- ref count -------------- ");
    values.entrySet()//.stream().filter(e -> e.getKey().startsWith("ref://"))
      .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
  }
}
