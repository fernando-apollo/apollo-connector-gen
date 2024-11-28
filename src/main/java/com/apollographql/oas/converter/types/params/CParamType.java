package com.apollographql.oas.converter.types.params;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.gen.TypeGen;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

public class CParamType extends CType {

  private final boolean required;
  private final Object defaultValue;

  private String resultType;

  public CParamType(String name, Schema schema, boolean required, Object defaultValue ) {
    super(name, schema, CTypeKind.PARAM, true);
    this.required = required;
    this.defaultValue = defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public String getResultType() {
    return resultType;
  }

  public void setResultType(String resultType) {
    this.resultType = resultType;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    writer.write(getName());
    writer.write(": ");
    writer.write(getParamType());

    if (isRequired()) {
      writer.write("!");
    }

    if (getDefaultValue() != null) { // best effort here..
      writer.write(" = \"");
      writer.write(getDefaultValue().toString());
      writer.write("\"");
    }
  }

  private String getParamType() {
    if (getResultType() != null) return getResultType();

    return TypeGen.getGQLScalarType(getSchema());
  }

  @Override
  public String toString() {
    return "CParamType{" +
      "name=" + getName() +
      ", required=" + required +
      ", defaultValue=" + defaultValue +
      ", props=" + props +
      ", resultType=" + resultType +
      '}';
  }
}
