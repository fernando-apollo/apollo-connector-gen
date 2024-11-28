package com.apollographql.connector.oas.visitor;

import com.apollographql.connector.oas.gen.*;
import com.apollographql.oas.converter.context.Context;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;
import java.util.Optional;

import static com.apollographql.oas.converter.utils.Trace.print;
import static com.apollographql.oas.converter.utils.Trace.printNode;

public abstract class AbstractVisitor {
  public static final String COMPONENTS_PREFIX = "#/components/schemas/";
  public static final String RESPONSES_PREFIX = "#/responses/schemas/";

  private OpenAPI parser;
  protected int indent = 0;

  public Context getContext() {
    return context;
  }

  protected final Context context;

  public AbstractVisitor(OpenAPI parser) {
    this.parser = parser;
    this.context = new Context();
  }

  public OpenAPI getParser() {
    return parser;
  }

  abstract public String visit();

  protected Gen visitSchema(final String name, final Schema schema) {
    print(indent, "->[visitSchema]", "Name: " + name + ", schema class: " + schema.getClass().getSimpleName());

    final String ref = schema.get$ref();
    print(indent, "  [visitSchema]", "Schema ref: " + ref + ", is component: " + isComponent(ref));

    Gen result;
    if (isComponent(ref)) {
      indent++;
      result = visitComponentSchemaRef(ref);
      indent--;
    }
    else {
      final String type = schema.getType();
      print(indent, "  [visitSchema]", "Schema type: " + type);

      if (type.equals("object")) {
        indent++;
        result = visitObjectSchema(name, schema);
        indent--;
      }
      else if (type.equals("array")) {
        indent++;
        result = visitArraySchema(name, schema);
        indent--;
      }
      else if (gqlScalar(type) != null) {
        // we can do a direct conversion
        result = new GenScalar(name, gqlScalar(type));
      }
      else {
        throw new IllegalStateException("Cannot handle type yet: " + type);
      }
    }

    print(indent, "<-[visitSchema]", "Name: " + name);
    assert result != null : "Could not create valid Gen node";
    return result;
  }

  private Gen visitArraySchema(String name, Schema parentSchema) {
    printNode(indent, "->[visitArraySchema]", name, parentSchema.getItems());

    Gen result = visitSchema(name, parentSchema.getItems());

    printNode(indent, "<-[visitArraySchema]", name, parentSchema.getItems());
    return new GenArray(result);
  }

  private Gen visitObjectSchema(String name, Schema schema) {
    printNode(indent, "->[visitObjectSchema]", name, schema);

    final GenObject result = new GenObject(name);
    final Map<String, Schema> properties = schema.getProperties();

    for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
      indent++;
      result.add(visitProperty(propertyEntry));
      indent--;
    }

    printNode(indent, "<-[visitObjectSchema]", name, schema);
    return result;
  }

  private GenField visitProperty(Map.Entry<String, Schema> property) {
    printNode(indent, "->[visitProperty]", property.getKey(), property.getValue());

    indent++;
    final Gen result = visitSchema(property.getKey(), property.getValue());
    indent--;

    printNode(indent, "<-[visitProperty]", property.getKey(), property.getValue());
    return new GenField(property.getKey(), result);
  }

  protected Gen visitComponentSchemaRef(String ref) {
    print(indent, "->[visitComponentRef]", ref);

    Optional<Map.Entry<String, Schema>> first = getComponentSchemas().entrySet().stream().filter(e -> ref.equals(COMPONENTS_PREFIX + e.getKey())).findFirst();

    Gen result = null;
    if (first.isPresent()) {
      print(indent, "->[visitComponentRef]", "found component schema '" + first.get().getKey() + "'");
      indent++;
      result = visitSchema(first.get().getKey(), first.get().getValue());
      indent--;
    }

    print(indent, "<-[visitComponentRef]", ref);
    return result;
  }

  private Map<String, Schema> getComponentSchemas() {
    return getComponents().getSchemas();
  }

  private Components getComponents() {
    return getParser().getComponents();
  }

  protected static boolean isComponent(String ref) {
    return ref != null && ref.startsWith(COMPONENTS_PREFIX);
  }

  private static String gqlScalar(String type) {
    switch (type) {
      case "string", "date", "date-time" -> {
        return "String";
      }
      case "integer" -> {
//        return schema.getFormat() != null && schema.getFormat().equals("int64") ? "String" : "Int";
        return "Int"; // this is actually wrong, unfortunately.
      }
      case "number" -> {
        return "Float";
      }
      case "boolean" -> {
        return "Boolean";
      }
      default -> {
        return null;
      }
    }
  }
}
