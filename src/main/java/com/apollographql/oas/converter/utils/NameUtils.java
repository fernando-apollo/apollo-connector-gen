package com.apollographql.oas.converter.utils;

import com.apollographql.oas.converter.visitor.ComponentResponsesVisitor;
import com.apollographql.oas.converter.visitor.ComponentSchemasVisitor;
import io.swagger.v3.oas.models.Operation;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

@Deprecated
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
    return StringUtils.uncapitalize(capitaliseParts(param, "[-_\\.]"));
  }

  @Deprecated
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

  @Deprecated
  public static String genSyntheticType(final String name) {
    return name + "Response";
  }

  public static String getRefName(final String ref) {
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
        final String name = capitaliseParts(p.getName(), "[-.]");
        return String.format("By%s", StringUtils.capitalize(name));
      })
      .toList() : Collections.emptyList();

    String result = "";

    // TODO: should be use the operationId instead?
    result = formatPath(path, parameters);

    return StringUtils.uncapitalize(result);
  }

  private static String formatPath(final String path, final List<String> parameters) {
    if (path == null || path.isEmpty()) {
      return path; // Return as-is if null or empty
    }

    // Step 1: Remove parameters enclosed in `{}`.
    String cleanedPath = path.replaceAll("\\{[^}]*}", String.join("", parameters));
    cleanedPath = capitaliseParts(cleanedPath, "[-.]");

    // Step 2: Split the path into parts and capitalize each part.
    return capitaliseParts(cleanedPath, "/");
  }

  public static String sanitiseField(final String name) {
    final String fieldName = name.startsWith("@") ? name.substring(1) : name;

    return genParamName(fieldName);
  }

  public static String sanitiseFieldForSelect(final String name) {
    final String fieldName = name.startsWith("@") ? name.substring(1) : name;

    final String sanitised = genParamName(fieldName);

    if (sanitised.equals(name)) {
      return sanitised;
    }
    else {
      final boolean needsQuotes = fieldName.matches(".*[_\\-\\.].*") || name.startsWith("@");
      final StringBuilder builder = new StringBuilder();
      builder.append(sanitised)
        .append(": ");

      if (needsQuotes) {
        builder.append('"');
      }

      builder.append(name.startsWith("@") ? name : fieldName);

      if (needsQuotes) {
        builder.append('"');
      }

      return builder.toString();
    }
  }

}
