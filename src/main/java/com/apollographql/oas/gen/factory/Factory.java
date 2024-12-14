package com.apollographql.oas.gen.factory;

import com.apollographql.oas.converter.utils.GqlUtils;
import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.nodes.*;
import com.apollographql.oas.gen.nodes.params.Param;
import com.apollographql.oas.gen.nodes.props.Prop;
import com.apollographql.oas.gen.nodes.props.PropArray;
import com.apollographql.oas.gen.nodes.props.PropRef;
import com.apollographql.oas.gen.nodes.props.PropScalar;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.List;

@SuppressWarnings({"ALL", "unchecked"})
public class Factory {
  public static GetOp createGetOperation(String name, Operation get) {
    final GetOp result = new GetOp(name, get);
    result.setOriginalPath(name);
    result.setSummary(get.getSummary());

    return result;
  }

  public static Type fromSchema(final Type parent, final Schema schema) {
    Type result = null;
    if (schema.get$ref() != null) {
      result = new Ref(parent, schema.get$ref(), schema.get$ref());
    }
    else if (schema instanceof ArraySchema) {
      result = new Array(parent, schema.getItems());
    }
    else if (schema instanceof ObjectSchema) {
      result = new Obj(parent, schema.getName(), schema);
    }
    else if (schema instanceof ComposedSchema) {
      result = new Composed(parent, schema.getName(), schema);
    }
    else {
      final String type = schema.getType();
      if (type != null) {
        if (type.equals("array")) {
          throw new IllegalArgumentException("Should have been handled already? " + type + ", schema: " + schema);
        }
        else if (schema.getEnum() != null) {
          result = new En(parent, schema, schema.getEnum());
        }
        else if (GqlUtils.gqlScalar(type) != null) { // scalar includes object => JSON
          result = new Scalar(parent, schema);
        }
        else {
          throw new IllegalArgumentException("Cannot handle property type " + type + ", schema: " + schema);
        }
      }
      else {
        throw new IllegalArgumentException("Cannot handle property with schema: " + schema);
      }
    }

    if (result != null) {
      parent.add(result);
    }
    else {
      throw new IllegalStateException("Not yet implemented for " + schema);
    }

    return result;
  }

  public static Prop fromProperty(Type parent, String propertyName, Schema propertySchema) {
    final String type = propertySchema.getType();

    Prop prop;
    if (type == null && propertySchema.get$ref() != null) {
      prop = new PropRef(parent, propertyName, propertySchema, propertySchema.get$ref());
    }
    else if (type != null) {
      if (type.equals("array")) {
        final PropArray array = new PropArray(parent, propertyName, propertySchema);
        final Prop items = fromProperty(array, "items", propertySchema.getItems());
        array.setItems(items);
        prop = array;
      }
      else if (GqlUtils.gqlScalar(type) != null) { // scalar includes object => JSON
        prop = new PropScalar(parent, propertyName, GqlUtils.gqlScalar(type), propertySchema);
      }
      else {
        throw new IllegalArgumentException("Cannot handle property type " + type);
      }
    }
    else {
      // we'll assume the type has no type, and we'll use the JSON scalar instead
      prop = new PropScalar(parent, propertyName, "JSON", propertySchema);
    }

    return prop;
  }

  public static Param fromParam(final Context context, final Type parent, final Parameter p) {
    if (p.get$ref() != null) {
      throw new IllegalStateException("Don't know how to handle ref params yet: " + p);
    }

    final Schema schema = p.getSchema();
    var required = p.getRequired() != null && p.getRequired().equals(Boolean.TRUE);

    return new Param(parent, p.getName(), schema, required, schema.getDefault());
  }

  public static Type fromResponse(final Context context, final Type parent, final ApiResponse response) {
    return new ResponseRef(parent, response.get$ref());
  }

  public static Type fromUnion(final Context context, final Type parent, final List<Schema> oneOfs) {
    final Union union = new Union(parent, parent.getSimpleName(), oneOfs);
    parent.add(union);

    return union;
  }
}
