package com.apollographql.connector.oas.visitor;

import com.apollographql.connector.oas.gen.Gen;
import com.apollographql.oas.converter.context.Context;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.apollographql.oas.converter.utils.Trace.print;
import static com.apollographql.oas.converter.utils.Trace.warn;

public class PathsVisitor extends AbstractVisitor {

  public String getTargetPath() {
    return targetPath;
  }

  private final String targetPath;

  public PathsVisitor(OpenAPI parser, String path) {
    super(parser);
    this.targetPath = path;
  }

  @Override
  public String visit() {
    // find target operation
    final Optional<Map.Entry<String, PathItem>> first = this.getParser().getPaths().entrySet().stream()
      .filter(e -> e.getKey().equals(this.getTargetPath()))
      .findFirst();

    if (first.isPresent()) {
      return visit(first.get().getKey(), first.get().getValue());
    }

    return null;
  }

  private String visit(String pathName, PathItem path) {

    if (path.getGet() != null) {
      visitGet(pathName, path.getGet());
    }

    return null;
  }

  private void visitGet(String pathName, Operation op) {
    print(indent, "->[visitGet]", pathName);

    final List<Parameter> parameters = op.getParameters();
    final String operationId = op.getOperationId();
    final ApiResponses responses = op.getResponses();

    StringWriter writer = new StringWriter();

    // we can only handle 'application/json' content type I guess
    for (Map.Entry<String, ApiResponse> e : responses.entrySet()) {
      if (e.getKey().equals("200")) {
        indent++;
        final Gen responseGen = visitResponse(e.getKey(), e.getValue());
        try {
          assert responseGen != null;
          responseGen.generate(writer);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        indent--;
      }
    }

    print(indent, "<-[visitGet]", pathName);
    System.out.println(writer.toString());
  }

  private Gen visitResponse(String code, ApiResponse response) {
    print(indent, "->[visitResponse]", code);
    final Context context = getContext();

    final String responseRef = response.get$ref();
    print(indent, "  [visitResponse]", responseRef != null ? "Found response ref" : "No response ref");

    final Content content = response.getContent();
    print(indent, "  [visitResponse]", "Response has content? " + (content != null ? "Yes" : "Nope"));

    final MediaType mediaType = content.get("application/json");
    if (mediaType == null) {
      warn(indent, "  [visitResponse]", "Content has no JSON media type? " + content);
      return null;
    }

    final Schema schema = mediaType.getSchema();
    indent++;
    final Gen result = visitSchema(code, schema);
    indent--;

    print(indent, "<-[visitResponse]", code);
    return result;
  }

}
