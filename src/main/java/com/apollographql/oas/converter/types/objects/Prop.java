package com.apollographql.oas.converter.types.objects;

import io.swagger.v3.oas.models.media.Schema;

import java.util.Objects;

public class Prop {
  private final String name;
  private final Schema schema;
  private final String entity;
  private final String type;
  private boolean required;

  public Prop(final String entity, final String name,  final String type, final Schema schema) {
    this.entity = entity;
    this.name = name;
    this.type = type;
    this.schema = schema;
  }

  public String getName() {
    return name;
  }

  public Schema getSchema() {
    return this.schema;
  }

  public String getEntity() {
    return entity;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Prop prop = (Prop) o;
    return Objects.equals(getName(), prop.getName()) && Objects.equals(getEntity(), prop.getEntity());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getEntity());
  }

  @Override
  public String toString() {
    return "Prop{" +
      "name='" + name + '\'' +
      ", type='" + type + '\'' +
      ", entity='" + entity + '\'' +
      '}';
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isRequired() {
    return required;
  }
}
