package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.gen.TypeGen;
import com.apollographql.oas.converter.types.objects.CObjectType;
import com.apollographql.oas.converter.types.objects.CSchemaType;
import com.apollographql.oas.converter.types.objects.Prop;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import com.apollographql.oas.converter.context.Context;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public abstract class CType {
  private final String name;
  private final Schema schema;
  private final CTypeKind kind;
  private final boolean resolved;
  protected Map<String, Prop> props = new LinkedHashMap<>();

  public CType(String name, Schema schema, CTypeKind kind, boolean resolved) {
    this.name = name;
    this.schema = schema;
    this.kind = kind;
    this.resolved = resolved;

    if (schema != null) {
      if (schema.getProperties() != null) {
        addPropertiesFromSchema(schema.getProperties());
      }

      final List<String> required = schema.getRequired();
      if (required != null) {
        final Map<String, Prop> propMap = this.getProps();

        required.stream()
          .map(propMap::get)
          .filter(Objects::nonNull)
          .forEach(prop -> prop.setRequired(true));
      }
    }
  }

  public static CType fromObject(String name, ObjectSchema schema) {
    if (name == null) throw new IllegalArgumentException(("CType name cannot be null"));
    return new CObjectType(name, schema);
  }

  public static CType fromSchema(Schema schema) {
    return new CSchemaType(schema);
  }

  public static CType fromEnum(String name, Schema schema, List<String> items) {
    return new CEnumType(name, schema, items);
  }

  @Override
  public String toString() {
    return "CType{" +
      "name='" + name + '\'' +
      ", kind=" + kind +
//      ", props=" + this.props.keySet().stream().map(e -> String.format("%s\n", e)).toList() +
      ", props=" + this.props.size() +
      '}';
  }

  public Schema getSchema() {
    return schema;
  }

  public CTypeKind getKind() {
    return kind;
  }

  public boolean getResolved() {
    return resolved;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (CType) obj;
    return Objects.equals(this.name, that.name) &&
      Objects.equals(this.schema, that.schema) &&
      Objects.equals(this.kind, that.kind) &&
      this.resolved == that.resolved;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, schema, kind, resolved);
  }

  public String getName() {
    return name != null ? name : "<anonymous>";
  }

  protected void addPropertiesFromSchema(Map<String, Schema> properties) {
    this.props = properties.entrySet().stream()
      .map(entry -> new Prop(this.getName(), entry.getKey(), entry.getValue().getType(), entry.getValue()))
//      .collect(Collectors.toMap(Prop::getName, prop -> prop));
      .collect(Collectors.toMap(Prop::getName, prop -> prop,
        (v1, v2) -> { throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));},
        TreeMap::new)
      );
  }

  public Map<String, Prop> getProps() {
    return props;
  }

  public void generate(Context context, Writer writer) throws IOException {
    System.out.println(String.format("[composed] -> object: %s", this.getName()));

    final TypeGen typeGen = new TypeGen(this, context);

    for (Prop prop : this.getProps().values()) {
      System.out.println(String.format("[composed] \t -> property: %s (parent: %s)", prop.getName(), prop.getEntity()));

      final Schema schema = prop.getSchema();

      if (schema.getType() == null) {
        if (schema.get$ref() != null) {
          String ref = schema.get$ref();
          typeGen.addField(prop, ref, schema.getDescription(), prop.getEntity().equals(this.getName()) ? null : prop.getEntity());
        }
        else {
          System.err.println(String.format("[warn] field '%s' has no type and no $ref - %s", prop.getName(), schema));
          // this means we have not found a type nor a href - not sure what the default is, but we'll default it to string
          typeGen.addField(prop, "JSON", schema.getDescription(), prop.getEntity());
        }
        continue;
      }

      if (schema.getType().equals("array")) {
        generateArray(context, typeGen, prop, schema);
      } else {
        typeGen.addScalar(prop, schema, schema.getType());
      }
    }

    typeGen.end();
    writer.write(typeGen.toString());
  }

  private static void generateArray(Context context, TypeGen typeGen, Prop prop, Schema schema) {
    // these are handled differently and we need to find the type in the schema
    final Schema itemsSchema = schema.getItems();

    if (itemsSchema != null) {
//      System.out.println("CObjectType.generate array with items " + itemsSchema.getClass().getSimpleName());
      final String ref = itemsSchema.get$ref();

      if (itemsSchema.getType() == null && ref != null) {
        final CType itemType = context.getType(ref);

        if (itemType != null) {
          typeGen.addField(prop, String.format("[ %s ]", itemType.getName()), itemsSchema.getDescription(), "array of items");
        }
        else {
          throw new IllegalStateException(String.format("Could not find type %s", ref));
        }
      }
      else if (itemsSchema.getType() != null) {
//        System.out.println("CType.generateArray ---------------- here: " + schema.getType() + ", items: " + itemsSchema.getType());;
        typeGen.addScalarArray(prop.getName(), itemsSchema);
      }
      else throw new IllegalStateException("What is the type for this?" + prop.getName() + ", schema:" + schema);
    }
  }

}
