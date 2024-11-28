package com.apollographql.oas.converter.types.operations;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.types.responses.CResponseObjectType;
import com.apollographql.oas.converter.types.responses.CResponseArrayType;
import com.apollographql.oas.converter.utils.NameUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class COperationType extends CType {
  private String resultType;
  private List<? extends CType> parameters = Collections.emptyList();
  private String originalPath;
  private String summary;

  public COperationType(String name, String resultType, List<? extends CType> parameters) {
    super(name, null, CTypeKind.OPERATION);
    this.resultType = resultType;
    this.parameters = parameters;
  }

  public String getResultType() {
    return resultType;
  }

  public void setResultType(String resultType) {
    this.resultType = resultType;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    System.out.println(String.format("->[operationType] -> begin: %s", this.getName()));

    final StringBuilder builder = new StringBuilder();
    if (getSummary() != null || getOriginalPath() != null) {
      builder.append("  \"\"\"\n").append("  ");

      if (getSummary() != null) {
        builder.append(getSummary()).append(" ");
      }

      if (getOriginalPath() != null) {
        builder.append("(").append(getOriginalPath()).append(")");
      }

      builder.append("\n  \"\"\"\n");
    }

    builder.append("  ").append(getName());

    // gen parameters
    generateParameters(context, builder);

    builder.append(": ");

    final String resultType = getResultType();
    System.out.println(String.format(" [operationType] -> resultType: %s", resultType));

    if (Context.isResponseType(resultType)) {
      final CType lookup = context.lookup(resultType);
      System.out.println(String.format(" [operationType] -> lookup: %s", lookup));

      switch (lookup.getKind()) {
        case RESPONSE_OBJECT -> {
          final String inlinedType = ((CResponseObjectType) lookup).getTypeRef();
          builder.append(NameUtils.getRefName(inlinedType));
        }
        case RESPONSE_ARRAY -> {
          final String inlinedType = ((CResponseArrayType) lookup).getItemsRef();
          builder.append("[ ").append(NameUtils.getRefName(inlinedType)).append(" ]");
        }
        default -> throw new IllegalStateException("Looked up response " + resultType + " is neither object nor array");
      }

      // I think we need to inline here the response type if it has been already declared in the
      // #/components/responses/ section - so let's start with a lookup
    } else {
      System.out.println(String.format(" [operationType] -> getResultType: %s", getResultType()));

      // we'll just throw the name in there and get on with it
      builder.append(getResultType());
    }

    builder.append("\n");
    writer.write(builder.toString());
    System.out.println(String.format("<-[operationType] -> end: %s", this.getName()));
  }

  private void generateParameters(Context context, StringBuilder builder) throws IOException {
    final List<? extends CType> parameters = getParameters();
    if (parameters.isEmpty()) return;

    builder.append("(");
    builder.append(String.join(", ", parameters.stream()
      .map(param -> generateParameter(context, param))
      .toList())
    );

    builder.append(")");
  }

  private static String generateParameter(Context context, CType parameter) {
    final StringWriter writer = new StringWriter();
    try {
      parameter.generate(context, writer);
    } catch (IOException e) {
      // ignore
    }
    return writer.toString();
  }

  public List<? extends CType> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return "COperationType{" +
      "name='" + getName() + '\'' +
      ", kind=" + getKind() +
      ", props=" + this.props.size() +
      ", resultType='" + resultType + '\'' +
      ", path='" + getOriginalPath() + '\'' +
      '}';
  }

  public void setOriginalPath(String originalPath) {
    this.originalPath = originalPath;
  }

  public String getOriginalPath() {
    return originalPath;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getSummary() {
    return summary;
  }

  @Override
  public Set<CType> getDependencies(Context context) {
    final Set<CType> deps = new LinkedHashSet<>();

    // our dependency is the result type
    if (Context.isResponseType(getResultType())) {
      deps.add(context.lookup(getResultType()));
    }

    return deps;
  }
}
