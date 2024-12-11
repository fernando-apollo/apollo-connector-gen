package com.apollographql.oas.converter.utils;

import io.swagger.v3.oas.models.media.Schema;

@Deprecated
public class GqlUtils {
  public static String getGQLScalarType(Schema schema) {
    switch (schema.getType()) {
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
        return"Boolean";
      }
      case "object" -> {
        return"JSON";
      }
      default -> throw new IllegalStateException("[getGQLScalarType] Cannot generate type = " + schema);
    }
  }

  public static String gqlScalar(String type) {
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
      case "object" -> {
        return"JSON";
      }
      default -> {
        return null;
      }
    }
  }
}
