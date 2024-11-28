package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import io.swagger.v3.oas.models.media.Schema;

public abstract class Prop {
  protected final String name;
  protected final String source;
  protected final Schema schema;
  protected boolean required;

  public Prop(final String name, final String source, final Schema schema) {
    this.name = name;
    this.source = source;
    this.schema = schema;
  }

  public String getName() {
    return name;
  }

  public Schema getSchema() {
    return this.schema;
  }

  public String getSource() {
    return source;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isRequired() {
    return required;
  }

  public abstract String getValue(Context context);
}
