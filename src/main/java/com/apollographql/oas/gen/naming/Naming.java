package com.apollographql.oas.gen.naming;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.converter.visitor.ComponentResponsesVisitor;
import com.apollographql.oas.converter.visitor.ComponentSchemasVisitor;
import org.apache.commons.lang3.StringUtils;

public class Naming {
  private static final Converter PARAM_CONVERTER =
    new ReplaceBracketsConverter(new CapitalisePartsConverted(new FinalFirstLowerCaseConverter()));

  private static final Converter TYPE_CONVERTER =
    new RemoveRefConverter(
    new CapitalisePartsConverted(new FinalFirstUpperCaseConverter()));

  private static final Converter REF_CONVERTER =
    new RemoveRefConverter(new FinalConverter());


  public static String genParamName(final String param) {
    return PARAM_CONVERTER.convert(param);
  }

  public static String genTypeName(final String name) {
    return TYPE_CONVERTER.convert(name);
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
      final boolean needsQuotes = fieldName.matches(".*[:_\\-\\.].*") || name.startsWith("@");
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

  public static String genArrayItems(final String name) {
    return StringUtils.capitalize(genParamName(name)) + "Item";
  }

  public static String getRefName(final String ref) {
    return ref != null ? REF_CONVERTER.convert(ref) : "";
  }

  public String convert(final String input) {
    final Converter chain = new BracketToCamelCaseConverter(
      new UpperCaseConverter(
        new FinalConverter()));

    return chain.convert(input);
  }

  // StringUtils.capitalize(NameUtils.genParamName(NameUtils.getRefName(getName())))

  public static void main(String[] args) {
    String input = "searchCriteria[filterGroups][0][filters][0][field]";
    System.out.println(new ReplaceBracketsConverter(
      new CapitalisePartsConverted(new FinalConverter())
    ).convert(input));

    input = "searchCriteria-filterGroups--0--filters--0--field";
    System.out.println(new CapitalisePartsConverted(new FinalConverter()).convert(input));
  }

  private static class FinalFirstLowerCaseConverter extends AbstractConverter {
    public FinalFirstLowerCaseConverter() {
      super(null);
    }

    @Override
    public String convert(final String input) {
      // does not call processNext(...)
      return StringUtils.uncapitalize(input);
    }
  }

  private static class RemoveRefConverter extends AbstractConverter {
    public RemoveRefConverter(final Converter converter) {
      super(converter);
    }

    @Override
    public String convert(final String input) {
      String result = input != null ? input : "";
      if (result.contains("#/components/schemas/"))
        result = result.replace("#/components/schemas/", "");

      if (result.contains("#/components/responses/"))
        result = result.replace("#/components/responses/", "");

      return processNext(result);
    }
  }

  private static class FinalFirstUpperCaseConverter extends AbstractConverter {
    public FinalFirstUpperCaseConverter() {
      super(null);
    }

    @Override
    public String convert(final String input) {
      return StringUtils.capitalize(input);
    }
  }
}

class CapitalisePartsConverted extends AbstractConverter {

  public CapitalisePartsConverted(final Converter next) {
    super(next);
  }

  @Override
  public String convert(final String input) {
    return processNext(capitaliseParts(input, "[\\-_\\.]"));
  }

  String capitaliseParts(final String cleanedPath, final String splitChar) {
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
}

class ReplaceBracketsConverter extends AbstractConverter {
  public ReplaceBracketsConverter(final Converter next) {
    super(next);
  }

  @Override
  public String convert(final String input) {
    return processNext(input.replaceAll("[\\[\\]]", "-"));
  }
}

class BracketToCamelCaseConverter extends AbstractConverter {
  public BracketToCamelCaseConverter(Converter next) {
    super(next);
  }

  @Override
  public String convert(String input) {
    final StringBuilder builder = new StringBuilder();

    boolean isFirst = true;
    var matcher = java.util.regex.Pattern.compile("[^\\[\\]]+").matcher(input);
    while (matcher.find()) {
      String token = matcher.group();
      if (isFirst) {
        builder.append(token);
        isFirst = false;
      }
      else {
        builder
          .append(Character.toUpperCase(token.charAt(0)))
          .append(token.substring(1));
      }
    }
    return processNext(builder.toString());
  }
}

// Example of an additional conversion step.
class UpperCaseConverter extends AbstractConverter {
  public UpperCaseConverter(Converter next) {
    super(next);
  }

  @Override
  public String convert(String input) {
    return processNext(input.toUpperCase());
  }
}

// Terminal converter that ends the chain.
class FinalConverter implements Converter {
  @Override
  public String convert(String input) {
    return input;
  }
}
