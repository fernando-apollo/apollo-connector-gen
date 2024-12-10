package com.apollographql.oas.converter.utils;

import com.apollographql.oas.converter.visitor.ComponentResponsesVisitor;
import com.apollographql.oas.converter.visitor.ComponentSchemasVisitor;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class NameUtils {
  public static String formatPath(String path) {
    if (path == null || path.isEmpty()) {
      return path; // Return as-is if null or empty
    }

    // Step 1: Remove parameters enclosed in `{}`.
    String cleanedPath = path.replaceAll("\\{[^}]*}", "");

    // Step 2: Split the path into parts and capitalize each part.
    return capitaliseParts(cleanedPath, "/");
  }

  private static String capitaliseParts(final String cleanedPath, final String splitChar) {
    String[] parts = cleanedPath.split(splitChar);
    StringBuilder formattedPath = new StringBuilder();

    for (String part : parts) {
      if (!part.isEmpty()) {
        // Capitalize the first letter of each part
        formattedPath //.append("/")
          .append(part.substring(0, 1).toUpperCase())
          .append(part.substring(1));
      }
    }

    // Step 3: Ensure the final string starts with a single slash.
    return formattedPath.toString();
  }


  public static String genParamName(final String param) {
    return StringUtils.uncapitalize(capitaliseParts(param, "[-_]"));
  }

  public static void main(String[] args) {
    String oasPath = "/users/{userId}/orders/{orderId}";
    String formattedPath = formatPath(oasPath);
    System.out.println(formattedPath); // Output: /Users/Orders
  }

  public static String genResponseType(final String path, final Operation operation) {
    if (operation == null)
      return null;

    final List<String> parameters = operation.getParameters().stream()
      .filter(parameter -> parameter.getRequired() != null && parameter.getRequired() && !parameter.getIn().equals("header"))
      .map(p -> String.format("By%s", StringUtils.capitalize(p.getName())))
      .toList();

    String result = "";
    // TODO: should be use the operationId instead?
    result = formatPath(path);

    if (parameters.size() > 0) {
      result = result + String.join("", parameters);
    }

    return result + "Response";
  }

  public static String genSyntheticType(final String name) {
    return name + "Response";
  }

  public static final String getRefName(final String ref) {
    if (ref == null) return null;

    if (ref.contains(ComponentSchemasVisitor.PREFIX))
      return ref.replace(ComponentSchemasVisitor.PREFIX, "");

    if (ref.contains(ComponentResponsesVisitor.PREFIX))
      return ref.replace(ComponentResponsesVisitor.PREFIX, "");

    return ref;
  }

  public static String genOperationName(String path, Operation operation) {

    final List<String> parameters = operation.getParameters() != null ? operation.getParameters().stream()
      .filter(parameter -> parameter.getRequired() != null && parameter.getRequired() && !parameter.getIn().equalsIgnoreCase("header"))
      .map(p -> {
        final String name = capitaliseParts(p.getName(), "-");
        return String.format("By%s", StringUtils.capitalize(name));
      })
      .toList() : Collections.emptyList();

    String result = "";

    // TODO: should be use the operationId instead?
    result = formatPath(path);

    if (parameters.size() > 0) {
      result = result + String.join("", parameters);
    }

    return StringUtils.uncapitalize(result);
  }
}
