package com.apollographql.oas.converter.visitor;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.objects.CComposedType;
import com.apollographql.oas.converter.types.objects.CUnionType;
import io.swagger.v3.oas.models.media.*;
import com.apollographql.oas.converter.types.CArrayType;
import com.apollographql.oas.converter.utils.Trace;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.apollographql.oas.converter.utils.Trace.print;

// we know the prefix for this is #
public class ComponentSchemasVisitor extends Visitor {
  public static final String PREFIX = "#/components/schemas/";
  private Map<String, Schema> schemas;

  public ComponentSchemasVisitor(final Context context, Map<String, Schema> schemas) {
    super(context);
    this.schemas = schemas;
  }

  @Override
  public void visit() {
    if (schemas == null) {
      return;
    }

    print(indent, "[visit]", "---------------------------- Component Schemas --------------------------");
    schemas.entrySet().forEach(entry -> walkNode(entry.getKey(), entry.getValue()));
  }

  @Override
  public void generate(Writer writer) throws IOException {
    print(indent, "[generate]", "---------------------------- Component Schemas --------------------------");
    for (Map.Entry<String, CType> entry : context.getTypes().entrySet()) {
      entry.getValue().generate(context, writer);
    }
  }

  private CType walkNode(String name, Schema schema) {
    if (schema instanceof ObjectSchema) {
      indent++;
      CType result = walkObjectNode(name, (ObjectSchema) schema);
      indent--;
      return result;
    } else if (schema instanceof ComposedSchema) {
      // could be allOf, anyOf, oneOf
      indent++;
      CType result = walkComposedNode(name, (ComposedSchema) schema);
      indent--;
      return result;
    } else if (schema instanceof StringSchema) {
      indent++;
      // ENUM
      if (!((StringSchema) schema).getEnum().isEmpty()) {
        return store(CType.fromEnum(fqName(name), schema, schema.getEnum()));
      }
      indent--;
    }
    else if (schema instanceof ArraySchema) {
      indent++;
      CType result = walkArrayNode(name, (ArraySchema) schema);
      indent--;
      return result;
    }
    throw new IllegalStateException("Cannot handle this yet: " + name + ", schema:" + schema);
//    return null;
  }

  private CType walkArrayNode(String name, ArraySchema schema) {
    Schema<?> itemsSchema = schema.getItems();
    if (itemsSchema.get$ref() != null) {
      // this is a sanity check mostly:
      CType lookup = context.lookup(itemsSchema.get$ref());
      if (lookup == null) {
        throw new IllegalStateException("Could not find items ref '" + itemsSchema.get$ref() + "'");
      }

      CType result = new CArrayType(name, schema, lookup.getName());
      return store(result);
    }

    throw new IllegalStateException("Cannot handle this yet: " + name + ", schema:" + schema);
  }

  private CType walkObjectNode(String name, ObjectSchema schema) {
    Trace.printNode(indent, "->[object]", "Found object type '" + name + "'", schema);
    indent++;

    if (context.containsType(name)) {
      Trace.printNode(indent, "[object] returning cached CType", name, schema);
      // we need to merge the two objects here
      CType newType = CType.fromObject(fqName(name), schema);
      CType sourceType = context.getType(name);

      sourceType.getProps().putAll(newType.getProps());
      store(sourceType);

      indent--;
      Trace.printNode(indent, "<-[object]", name, schema);
      return sourceType;
    }

    CType result = store(CType.fromObject(fqName(name), schema));
    assert result != null;

    indent--;
    Trace.printNode(indent, "<-[object]", name, schema);
    return result;
  }

  private CType walkComposedNode(String key, ComposedSchema schema) {
    if (schema.getAllOf() != null) {
      // this translates to a type with all the properties of the allOf schemas
      return walkAllOfNode(key, schema);
    } else if (schema.getOneOf() != null) {
      // this translates to a Union type
      return walkOneOfNode(key, schema);
    } else {
      throw new IllegalStateException("Walker.walkComposedNode: unsupported composed schema: " + schema.getClass().getSimpleName());
    }

//    return null;
  }

  private CType walkOneOfNode(String name, ComposedSchema schema) {
    final List<Schema> oneOfs = schema.getOneOf();
    Trace.printNode(indent, "[oneOf]", String.format("OneOf %s with size: %d", name, oneOfs.size()), schema);

    final CUnionType result = new CUnionType(fqName(name), schema);
    indent++;

    for (final Schema allOf : oneOfs) {
      if (allOf instanceof ObjectSchema) {
        throw new IllegalStateException("Walker.[oneOf]: ObjectSchema not supported in OneOf - generate anonymous type ??");
      }

      final String refName = allOf.get$ref();
      final Schema refSchema = lookupNode(refName);
      if (refSchema != null) {
        // this is different from the AllOf - in this case we only need to add the refName
        result.addType(refName);
      } else {
        throw new IllegalStateException("Walker.[oneOf]: could not find schema for: " + refName);
      }
    }

    indent--;

    store(result);

    return result;
  }

  private CType walkAllOfNode(String name, ComposedSchema schema) {
    final List<Schema> allOfs = schema.getAllOf();
    final List<String> refs = allOfs.stream().map(Schema::get$ref).toList();

    Trace.printNode(indent, "->[allOf]", String.format("Found type '%s' of: %d - refs: %s", name, allOfs.size(), refs), schema);

    final CComposedType result = new CComposedType(fqName(name), schema);
    indent++;
    for (int i = 0; i < allOfs.size(); i++) {
      final Schema allOf = allOfs.get(i);
      Trace.print(indent, "[allOf]", "ref(" + i + ") class: " +
        allOf.getClass().getSimpleName() + ", ref? " + allOf.get$ref());

      // is this an ObjectSchema?
      if (allOf instanceof ObjectSchema) {
        final CType objectType = walkObjectNode(name, (ObjectSchema) allOf);
        Trace.print(indent, "[allOf]", "In '" + name + "' - add properties from [object] " + objectType);
        // we need to collect the properties of the CType here
        result.addPropertiesFrom(objectType);
        continue;
      }

      if (allOf.get$ref() == null) {
        // might be an anon schema and it might have properties
        if ((Map<String, Schema>) allOf.getProperties() != null) {
          result.addPropertiesFrom(CType.fromSchema(allOf));
          continue;
        }
        throw new IllegalStateException("Walker.walkAllOfNode: could not find schema for: " + allOf);
      }

      // we need to walk the AllOf schemas too
      final String refName = allOf.get$ref();

      // let's do a quick lookup in the context first
      final CType cachedType = context.getType(refName);
      if (cachedType != null) {
        Trace.print(indent, "[allOf]", "Found cached type for " + refName);
        result.addPropertiesFrom(Objects.requireNonNull(cachedType));
        continue;
      }

      final Schema allOfNode = lookupNode(refName);

      if (allOfNode != null) {
        // now we need to collect the properties of the schema
        final CType walkedNode = walkNode(refName, allOfNode);

        Trace.print(indent, "[allOf]", "In '" + name + "' - add properties from [lookup] " + walkedNode);
        assert walkedNode != null : "Null CType for ref " + refName;

        result.addPropertiesFrom(Objects.requireNonNull(walkedNode));
      } else {
        // we should issue a warning here
        throw new IllegalStateException("Walker.walkAllOfNode: could not find schema for: " + allOf);
      }
    }

    store(result);
    indent--;

    Trace.printNode(indent, "<-[allOf]", String.format("End of '%s'", name), schema);

    return result;
  }

  private Schema lookupNode(String refName) {
    Optional<Map.Entry<String, Schema>> first = this.schemas.entrySet().stream()
      .filter(entry -> (PREFIX + entry.getKey()).equals(refName))
      .findFirst();

    return first.map(Map.Entry::getValue).orElse(null);
  }

  protected CType store(CType type) {
    Trace.print(indent, "[store]", "storing " + type.getName() + " with " + type);
    return context.putType(type.getName(), type);
  }

  private static String fqName(String name) {
    if (name.startsWith(PREFIX)) return name;
    return PREFIX + name;
  }
}
