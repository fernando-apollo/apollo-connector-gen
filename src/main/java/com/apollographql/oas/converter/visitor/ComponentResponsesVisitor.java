package com.apollographql.oas.converter.visitor;

import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.responses.CResponseArrayType;
import com.apollographql.oas.converter.types.responses.CResponseObjectType;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.utils.Trace;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import static com.apollographql.oas.converter.utils.Trace.print;
import static com.apollographql.oas.converter.utils.Trace.warn;

/**
 * Responses will end up in the Context too as Types - for now - so we can look them up. The resolution should be
 * similar to what we did for the Component.Schemas visitor.
 */
public class ComponentResponsesVisitor extends Visitor {
  public static final String PREFIX = "#/components/responses/";

  private final Map<String, ApiResponse> responses;
  private int indent = 0;

  public ComponentResponsesVisitor(final Context context, Map<String, ApiResponse> responses) {
    super(context);
    this.responses = responses;
  }

  @Override
  public void visit() {
    if (responses == null) {
      return;
    }

    print(indent, "[visit]", "---------------------------- Component Responses --------------------------");
    this.responses.entrySet().stream()
      .filter(entry -> {
        final ApiResponse response = entry.getValue();

        if (response.getContent() == null && response.get$ref() == null) {
          warn(indent, "[visit]", "Response with no content nor ref: " + entry.getKey() + ", check #/components/responses/" + entry.getKey());
        }

        return response.getContent() != null || (response.getContent() == null && response.get$ref() != null);
      })
      .toList()
      .forEach(this::visitResponse);
  }

  @Override
  public void generate(Writer writer) throws IOException {
    // TODO
//    throw  new IllegalStateException("Not implemented yet");
    print(indent, "[generate]", "---------------------------- Component Responses (" + context.getResponses().size() + ") --------------------------");
    for (Map.Entry<String, CType> entry : context.getResponses().entrySet()) {
      entry.getValue().generate(context, writer);
    }
  }

  private void visitResponse(Map.Entry<String, ApiResponse> entry) {
    final String responseName = entry.getKey();
    final ApiResponse response = entry.getValue();
    print(indent, "->[" + responseName + "]", "walking through: '" + responseName +
      "' with value " + response.getClass().getSimpleName());

    final Content content = response.getContent();
    final MediaType mediaType = content.get("application/json");

    if (mediaType == null) { // bail
      warn(indent, "  [" + responseName + "]", "no mediaType found for content, bailing out!");
      return;
    }

    // let's check first if we have a schemaRef
    final Schema schema = mediaType.getSchema();
    final String schemaRef = schema.get$ref();
    if (schemaRef != null) {
      CType cType = lookupRef(schemaRef);
      if (cType == null) throw new IllegalStateException("Could not find schemaRef '" + schema + "'");

      store(new CResponseObjectType(fqName(responseName), schemaRef));
    }
    // now let's check the ApiResponse
    else if (schema instanceof ObjectSchema) {
      print(indent, "  [" + responseName + "]", "schema is ObjectSchema");
    } else if (schema instanceof ArraySchema) {
      indent++;
      CType returnType = visitArraySchema(responseName, (ArraySchema) schema);
      store(returnType);
      indent--;
    } else {
      throw new IllegalStateException("Can't handle " + schema.getClass().getSimpleName());
    }

    print(indent, "<-[" + responseName + "]", "end: '" + responseName + "'");
  }

  private CType visitArraySchema(final String responseName, final ArraySchema schema) {
    print(indent, "->[" + "visitArraySchema" + "]", "analyzing array");

    CType result;
    final String itemsRef = schema.getItems().get$ref();

    if (itemsRef != null) {
      print(indent, "  [" + "visitArraySchema" + "]", "found items ref: " + itemsRef);
      final CType itemsType = lookupRef(itemsRef);
      if (itemsType == null) {
        throw new IllegalStateException("Could not find itemsType '" + itemsRef + "'");
      }
      else {
        result = new CResponseArrayType(fqName(responseName), itemsRef);
      }
    }
    else {
      final Schema<?> itemsSchema = schema.getItems();
      print(indent, "  [" + "visitArraySchema" + "]", "checking items schema: " + itemsSchema.getClass().getSimpleName());
      throw new IllegalStateException("Cannot proceed from here? " + itemsSchema.getClass().getSimpleName());
    }

    print(indent, "<-[" + "visitArraySchema" + "]", "analyzing array");
    return result;
  }
  protected CType store(CType type) {
    Trace.print(indent, "[store]", "storing " + type.getName() + " with " + type);
    context.putResponse(type);
    return type;
  }

  private static String fqName(String name) {
    if (name.startsWith(PREFIX)) return name;
    return PREFIX + name;
  }
}
