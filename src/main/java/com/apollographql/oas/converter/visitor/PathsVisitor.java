package com.apollographql.oas.converter.visitor;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.context.DependencySet;
import com.apollographql.oas.converter.types.CMapType;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.operations.COperationType;
import com.apollographql.oas.converter.types.params.CParamType;
import com.apollographql.oas.converter.types.responses.CResponseArrayType;
import com.apollographql.oas.converter.types.responses.CResponseObjectType;
import com.apollographql.oas.converter.utils.GqlUtils;
import com.apollographql.oas.converter.utils.Trace;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import com.apollographql.oas.converter.utils.NameUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static com.apollographql.oas.converter.utils.Trace.print;
import static com.apollographql.oas.converter.utils.Trace.warn;

/**
 * Responses will end up in the Context too as Types - for now - so we can look them up. The resolution should be
 * similar to what we did for the Component.Schemas visitor.
 */
public class PathsVisitor extends Visitor {
  private Paths paths;

  public PathsVisitor(final Context context, Paths paths) {
    super(context);
    this.paths = paths;
  }

  @Override
  public void visit() {
    print(indent, "[visit]", "---------------------------- Operations --------------------------");
    paths.entrySet().stream()
      .filter(entry -> entry.getValue().getGet() != null)
      .forEach(entry -> visitPath(entry.getKey(), entry.getValue()));
  }

  @Override
  public void generate(Writer writer) throws IOException {
    if (context.getOperations().isEmpty()) {
      return;
    }

    print(indent, "[generate]", "---------------------------- Operations --------------------------");
    writer.write("type Query {\n");

    for (Map.Entry<String, CType> entry : context.getOperations().entrySet()) {
      entry.getValue().generate(context, writer);
    }

    writer.write("}\n\n");
  }

  private void visitPath(String path, final PathItem item) {
    print(indent, "->[visitPath]", "analyzing path: " + path);
    final Operation getOp = item.getGet();
    final String summary = getOp.getSummary();

    // this is actually a synthetic name for our GraphQL response
    print(indent, "  [visitPath]", "operationId: " + getOp.getOperationId());

    final List<? extends CType> parameters = visitParameters(getOp);

    // only 200 for now
    for (Map.Entry<String, ApiResponse> e : getOp.getResponses().entrySet()) {
      if (!(e.getKey().equals("200") || e.getKey().equals("default"))) {
        Trace.print(indent, "  [visitPath]", "Ignoring response '" + e.getKey() + "'");
        continue;
      }

      indent++;
      final ApiResponse response = e.getValue();

      // check ref first
      final String responseRef = response.get$ref();
      if (responseRef != null) {
        final COperationType resultType = visitResponseRef(path, getOp, responseRef, parameters);
        resultType.setSummary(summary);

        context.putOperation(resultType);
      } else {
        final Content content = response.getContent();

        Optional<Map.Entry<String, MediaType>> first = content.entrySet().stream()
          .filter(entry -> entry.getKey().contains("application/json"))
          .findFirst();

        final String name = e.getKey();
        if (first.isEmpty()) { // bail
          indent--;
          warn(indent, "  [" + name + "]", "no mediaType found for content application/json, bailing out!");
          continue;
        }

        // this is inline content - as in not using a ref for the response, which means we'll
        // have to parse it here instead - and 'walk' it. Also means we'll have to 'store' the content in the
        // responses section and synthesize a return value for it - best way to do it I guess?
        final String operation = NameUtils.genOperationName(path, getOp);

        final Schema schema = first.get().getValue().getSchema();
        final String schemaRef = schema.get$ref();

        if (schemaRef != null) {
          final CType cType = lookupRef(schemaRef);
          if (cType == null) {
            throw new IllegalStateException("Could not find schemaRef '" + schema + "'");
          }

          final String resultType = NameUtils.genResponseType(path, getOp);
          final CResponseObjectType type = new CResponseObjectType(resultType, schemaRef);
          store(type);

          final COperationType opType = new COperationType(operation, schemaRef, parameters, type);
          opType.setOriginalPath(path);
          opType.setSummary(summary);

          context.putOperation(opType);
        }
        // now let's check the ApiResponse
        else if (schema instanceof ObjectSchema) {
          print(indent, "  [" + name + "]", "schema is ObjectSchema");
          throw new IllegalStateException("Cannot handle ObjectSchema yet for " + schema);
        } else if (schema instanceof ArraySchema) {
          indent++;
          CResponseArrayType returnType = (CResponseArrayType) visitArraySchema(name, (ArraySchema) schema);
          store(returnType);

          final COperationType opType = new COperationType(
            operation,
            "[" + NameUtils.getRefName(returnType.getItemsRef()) + "]",
            parameters,
            returnType
          );
          opType.setSummary(summary);
          opType.setOriginalPath(path);

          context.putOperation(opType);
          indent--;
        } else if (schema instanceof MapSchema) {
          indent++;
          // this is a work-around, and an easy one at that. we can potentially have 3 scenarios here:
          // 1. we return a JSON which is dynamic - but that means that types will need to be defined manually
          // 2. we find the additional property and create a synthetic object, then use it as the op result
          // 3. the key is NOT a scalar, then we need to create the synthetic type (again) and use it as the
          //    key in the map
          final COperationType opType = new COperationType(operation, "JSON", parameters);
          opType.setOriginalPath(path);

          context.putOperation(opType);
          indent--;
        } else if (schema instanceof StringSchema) {
          // response is actually a scalar type, in this case String
          final COperationType opType = new COperationType(operation, GqlUtils.getGQLScalarType(schema), parameters);
          opType.setSummary(summary);
          opType.setOriginalPath(path);

          context.putOperation(opType);
        } else {
          throw new IllegalStateException("Can't handle " + schema.getClass().getSimpleName());
        }

        print(indent, "<-[visitPath]", "end: " + path);
      }
      indent--;
    }

    print(indent, "<-[visitPath]", "end: " + path);
  }

  private List<? extends CType> visitParameters(Operation getOp) {
    List<CParamType> parameters;
    if (getOp.getParameters() != null && !getOp.getParameters().isEmpty()) {
      indent++;
      parameters = getOp.getParameters().stream().map(this::visitParameter).toList();
      indent--;
    } else {
      parameters = Collections.emptyList();
    }
    return parameters;
  }

  private CParamType visitParameter(final Parameter p) {
    print(indent, "->[visitParameters]", "begin: " + p.getName());

    if (p.get$ref() != null) {
      print(indent, "->[visitParameters]", "found ref: " + p.get$ref());
      throw new IllegalStateException("Don't know how to handle ref params yet: " + p);
    }

    final Schema schema = p.getSchema();
    print(indent, "->[visitParameters]", "param type: " + schema.getType() + ", schema class: " + schema.getClass().getSimpleName());

    var required = p.getRequired() != null && p.getRequired().equals(Boolean.TRUE);

    CParamType result = new CParamType(p.getName(), schema, required, schema.getDefault());
    result.setResultType(visitParameterType(p.getName(), schema));

    print(indent, "<-[visitParameters]", "end: " + p.getName() + ", result: " + result);
    return result;
  }

  private String visitParameterType(String name, final Schema schema) {
    print(indent, "->[visitParameterType]", "begin: " + name);

    String resultType;
    if (schema instanceof ArraySchema) {
      // this is where it starts to get tricky
      final Schema itemsSchema = schema.getItems();
      print(indent, "->[visitParameterType]", "found array, checking items schema " + itemsSchema.getType());

      // now we need a lookup just to check the value is actually there:
      resultType = "[" + GqlUtils.getGQLScalarType(itemsSchema) + "]";
    } else { // let's try scalar
      resultType = GqlUtils.getGQLScalarType(schema);
    }

    print(indent, "<-[visitParameterType]", "end: resultType = " + resultType);
    return resultType;
  }

  private CType visitMapSchema(String name, MapSchema schema) {
    final Object properties = schema.getAdditionalProperties(); //
    if (properties instanceof IntegerSchema integerSchema) {
      /*
      type KeyValue {
        key: String
        value: Int or String, depending
      }
      */
      final String gqlType = GqlUtils.getGQLScalarType(integerSchema);
      return new CMapType("IntegerMap", integerSchema, "key", gqlType);
    } else {
      throw new IllegalStateException("Can't handle properties " + properties);
    }
  }

  private COperationType visitResponseRef(String path, Operation getOp, String responseRef, List<? extends CType> parameters) {
    print(indent, "  [" + path + "]", "lookup responseRef: " + responseRef);
    CType lookup = context.lookup(responseRef);

    if (lookup == null) {
      print(indent, "  [" + path + "]", "found responseRef: " + responseRef);
      throw new IllegalStateException("Nope nope nope");
    }

    //
    final String operation = NameUtils.genOperationName(path, getOp);
    print(indent, "  [" + path + "]", "GQL operation: " +
      operation + ": " + NameUtils.getRefName(lookup.getName()));

    final COperationType opType = new COperationType(operation, lookup.getName(), parameters, lookup);
    opType.setOriginalPath(path);

    return opType;
  }

  private CType visitArraySchema(final String responseName, final ArraySchema schema) {
    print(indent, "->[" + "visitArraySchema" + "]", "analyzing array");

    CType result = null;
    final String itemsRef = schema.getItems().get$ref();

    if (itemsRef != null) {
      print(indent, "  [" + "visitArraySchema" + "]", "found items ref: " + itemsRef);
      final CType itemsType = lookupRef(itemsRef);
      if (itemsType == null) {
        throw new IllegalStateException("Could not find itemsType '" + itemsRef + "'");
      } else {
        result = new CResponseArrayType(responseName, itemsRef);
        ;
      }
    } else {
      final Schema<?> itemsSchema = schema.getItems();
      print(indent, "  [" + "visitArraySchema" + "]", "checking items schema: " + itemsSchema.getClass().getSimpleName());
      throw new IllegalStateException("Cannot proceed from here? " + itemsSchema.getClass().getSimpleName());
    }

    print(indent, "<-[" + "visitArraySchema" + "]", "analyzing array");
    return result;
  }

  public List<String> getPaths() {
    if (context.getOperations().isEmpty()) {
      return Collections.emptyList();
    }

    return context.getOperations().values().stream()
      .map(o -> ((COperationType) o).getOriginalPath())
      .toList();
  }

  @Override
  protected CType store(CType type) {
    print(indent, "[store]", "storing " + type.getName() + " with " + type);
    context.putResponse(type);
    return type;
  }

  public void generatePath(String path, Writer writer) throws IOException {
    indent = 0;
    if (context.getOperations().isEmpty()) {
      return;
    }

    List<CType> entries = context.getOperations().values().stream()
      .filter(e -> {
        final COperationType operation = (COperationType) e;
        print(indent, "[generatePath]", "checking " + operation.getOriginalPath() + " with " + path);
        return operation.getOriginalPath().equals(path);
      }).toList();

    if (entries.isEmpty()) {
      warn(indent, "[generatePath]", "Path '" + path + "' not found - please check");
      return;
    }

    if (entries.size() > 1) throw new IllegalStateException("Multiple paths found");

    writeConnector(path, writer, entries);
  }

  private void writeConnector(String path, Writer writer, List<CType> entries) throws IOException {
    final Set<String> generatedSet = context.getGeneratedSet();
    writeSchema(path, writer, generatedSet, entries);
  }

  private void writeSchema(String path, Writer writer, Set<String> generatedSet, List<CType> entries) throws IOException {
    print(indent, "[writeSchema]", "---------------------------- Operations --------------------------");
    writeDirectives(writer);

    final DependencySet dependencies = new DependencySet();
    final Set<CType> skipSet = new LinkedHashSet<>();

    collectDependencies(entries.get(0), dependencies, skipSet);

    for (final CType type : dependencies.get()) {
      if (!skipSet.contains(type) && dependencies.getRefCount(type) == 1) {
        type.generate(context, writer);
      }
      else {
        print(indent, "-> [writeSchema]", "skipped " + type);
      }
      generatedSet.add(type.getName());
    }

    CType value = entries.get(0);
    print(indent, "-> [writeSchema]", "checking " + value.getName()
      + ", kind: " + value.getKind()
      + ", class: " + value.getClass().getSimpleName());

    if (indent == 0) {
      writer.write("type Query {\n");
    }

    if (!generatedSet.contains(value.getName())) {
      value.generate(context, writer);


      var spacing = " ".repeat(indent + 4);
      writer.append(spacing).append("@connect(\n");

      var newPath = path.replaceAll("\\{([a-zA-Z0-9]+)\\}", "{\\$args.$1}");
      spacing = " ".repeat(indent + 6);
      writer
        .append(spacing).append("source: \"api\"\n")
        .append(spacing).append("http: { GET: \"" + newPath + "\" }\n")
        .append(spacing).append("selection: \"\"\"\n");

//      writer.append("#### selection goes here\n");
      writeSelection(writer, entries);

      writer.append(spacing).append("\"\"\"\n");

      spacing = " ".repeat(indent + 4);
      writer.append(spacing).append(")\n");

      indent = 0;
    }

    if (indent == 0) {
      writer.write("}\n\n");
    }

    generatedSet.add(value.getName());
    print(indent, "<- [generateSingle]", "end " + value.getName());
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

  private void writeSelection(Writer writer, List<CType> entries) throws IOException {
    print(indent, "[generatePath]", "---------------------------- Selection --------------------------");
    final DependencySet dependencies = new DependencySet();
    entries.get(0).select(context, writer, dependencies);
  }

  private void collectDependencies(CType node, DependencySet dependencySet, Set<CType> skipSet) {
    print(indent, "-> [gatherDependencies]", "checking " + node.getName());

    final Set<CType> found = node.getDependencies(context);
    _printFound(node, dependencySet, found);

    final Set<CType> pendingSet = found.stream().filter(d -> !dependencySet.contains(d)).collect(Collectors.toSet());
    dependencySet.addAll(pendingSet);

    // skip list - walk through each and find their skip list
    pendingSet.stream().map(c -> c.getSkipSet(context)).toList().forEach(skipSet::addAll);

    // depth-first
    for (final CType dependency : pendingSet) {
      indent++;
      collectDependencies(dependency, dependencySet, skipSet);
      indent--;
    }

    // then add all of them
    print(indent, "<- [gatherDependencies]", "end of " + node.getName());
  }

  private void _printFound(CType node, DependencySet stack, Set<CType> found) {
    print(indent, "   [stack]", "found " + found.size() + " deps for " + node.getName());

    for (final CType dependency : found) {
      if (stack.contains(dependency)) {
        print(indent, "   [stack]", "[WARN] Already added! : " + dependency.getName());
      } else {
        print(indent, "   [stack]", "New dependency: " + dependency.getName());
      }
    }
  }

}
