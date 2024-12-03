package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.context.DependencySet;
import com.apollographql.oas.converter.types.objects.CObjectType;
import com.apollographql.oas.converter.types.objects.CSchemaType;
import com.apollographql.oas.converter.types.props.ArrayProp;
import com.apollographql.oas.converter.types.props.Prop;
import com.apollographql.oas.converter.types.props.RefProp;
import com.apollographql.oas.converter.types.props.ScalarProp;
import com.apollographql.oas.converter.utils.GqlUtils;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public abstract class CType {
  private final String name;
  private final Schema schema;
  private final CTypeKind kind;
  private boolean resolved;

  protected Map<String, Prop> props = new LinkedHashMap<>();



  public CType(String name, Schema schema, CTypeKind kind) {
    this.name = name;
    this.schema = schema;
    this.kind = kind;

    if (schema != null) {
      if (schema.getProperties() != null) {
        addProperties(schema.getProperties());
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

  protected void addProperties(Map<String, Schema> properties) {
    for (var property : properties.entrySet()) {
      final String propertyName = property.getKey();
      final Schema propertySchema = property.getValue();

      final Prop prop = createProp(propertyName, propertySchema);
      if (prop != null) {
        getProps().put(propertyName, prop);
      }
    }
  }

  protected Prop createProp(String propertyName, Schema propertySchema) {
    final String source = getName();
    final String type = propertySchema.getType();

    Prop prop;
    if (type == null && propertySchema.get$ref() != null) {
      prop = new RefProp(propertyName, source, propertySchema, propertySchema.get$ref());
    }
    else if (type != null) {
      if (type.equals("array")) {
        final Prop items = createProp("items", propertySchema.getItems());
        prop = new ArrayProp(propertyName, source, propertySchema, items);
      }
      else if (GqlUtils.gqlScalar(type) != null) { // scalar includes object => JSON
        prop = new ScalarProp(propertyName, source, GqlUtils.gqlScalar(type), propertySchema);
      }
      else {
        throw new IllegalArgumentException("Cannot handle property type " + type + ", schema: " + schema);
      }
    }
    else {
      // we'll assume the type has no type, and we'll use the JSON scalar instead
      prop = new ScalarProp(propertyName, source, "JSON", propertySchema);
    }

    return prop;
  }

  public Map<String, Prop> getProps() {
    return props;
  }

  public abstract void generate(Context context, Writer writer) throws IOException;

  public Set<CType> getDependencies(Context context) {
    final Set<CType> deps = new HashSet<>();

    for (Prop p : getProps().values()) {
      final CType type = getDependenciesFromProp(context, p);
      if (type != null) deps.add(type);
    }

    return deps;
  }

  public Set<CType> getSkipSet(Context context) {
    return Collections.emptySet();
  }

  public static CType getDependenciesFromProp(Context context, Prop property) {
    if (property instanceof RefProp ref) {
      return context.lookup(ref.getRef());
    }
    else if (property instanceof ArrayProp array) {
      final Prop items = array.getItems();
      return getDependenciesFromProp(context, items);
    }
    return null;
  }

  public void select(Context context, Writer writer, DependencySet dependencies) throws IOException {
    throw new IllegalStateException("Not yet implemented for " + getClass().getSimpleName());
  }

  public String getSimpleName() {
    return NameUtils.getRefName(getName());
  }
}
