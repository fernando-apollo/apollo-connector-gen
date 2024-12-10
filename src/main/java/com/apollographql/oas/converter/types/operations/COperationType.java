package com.apollographql.oas.converter.types.operations;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.context.DependencySet;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.types.responses.CResponseArrayType;
import com.apollographql.oas.converter.types.responses.CResponseObjectType;
import com.apollographql.oas.converter.utils.NameUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

/**
 * @deprecated
 */
public class COperationType extends CType {
  private static final Logger logger = Logger.getLogger(COperationType.class.getName());
  
  private String result;
  private final List<? extends CType> parameters;
  private CType returnType;
  private String originalPath;
  private String summary;

  public COperationType(String name, String result, List<? extends CType> parameters) {
    super(name, null, CTypeKind.OPERATION);
    this.result = result;
    this.parameters = parameters;
  }

  public COperationType(String name, String result, List<? extends CType> parameters, CType returnType) {
    super(name, null, CTypeKind.OPERATION);
    this.result = result;
    this.parameters = parameters;
    this.returnType = returnType;
  }

  public String getResult() {
    return result;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    logger.log(FINE, String.format("->[operationType] -> begin: %s", this.getName()));

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

    final String resultType = getResult();
    logger.log(FINE, String.format(" [operationType] -> resultType: %s", resultType));

    if (Context.isResponseType(resultType)) {
      final CType lookup = context.lookup(resultType);
      logger.log(FINE, String.format(" [operationType] -> lookup: %s", lookup));

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
    }
    else if (Context.isSchemaType(resultType)) {
      builder.append(NameUtils.getRefName(getResult()));
    }
    else {
      logger.log(FINE, String.format(" [operationType] -> getResultType: %s", getResult()));

      // we'll just throw the name in there and get on with it
      builder.append(getResult());
    }

    builder.append("\n");
    writer.write(builder.toString());
    logger.log(FINE, String.format("<-[operationType] -> end: %s", this.getName()));
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
    if (Context.isResponseType(getResult())) {
      deps.add(context.lookup(getResult()));
    }
    else if (Context.isSchemaType(getResult())) {
      deps.add(context.lookup(getResult()));
    }
    else if (getReturnType() != null) {
      deps.add(getReturnType());
    }

    return deps;
  }

  @Override
  public void select(Context context, Writer writer, DependencySet stack) throws IOException {
    Set<CType> dependencies = getDependencies(context);

    for (CType dependency : dependencies) {
      dependency.select(context, writer, stack);
    }
  }

  @Override
  public String toString() {
    return "COperationType{" +
      "name='" + getName() + '\'' +
      ", kind=" + getKind() +
      ", props=" + this.props.size() +
      ", resultType='" + result + '\'' +
      ", path='" + getOriginalPath() + '\'' +
      '}';
  }

  public CType getReturnType() {
    return returnType;
  }
}
