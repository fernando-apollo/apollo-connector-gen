package com.apollographql.oas.converter.context;

import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.visitor.ComponentResponsesVisitor;
import com.apollographql.oas.converter.visitor.ComponentSchemasVisitor;

import java.util.*;

@Deprecated
public class Context {
  private final Map<String, CType> types = new TreeMap<>();
  private final Map<String, CType> operations = new TreeMap<>();

  private final Map<String, CType> responses = new TreeMap<>();
  private final Set<String> generatedSet = new LinkedHashSet<>();

  public static boolean isResponseType(String type) {
    return type.startsWith(ComponentResponsesVisitor.PREFIX);
  }

  public static boolean isSchemaType(String type) {
    return type.startsWith(ComponentSchemasVisitor.PREFIX);
  }

  public CType putType(String name, CType value) {
    types.put(name, value);
    return value;
  }

  @Override
  public String toString() {
    return types.toString();
  }

  public boolean containsType(String name) {
    return types.containsKey(name);
  }

  public CType getType(String name) {
    return types.get(name);
  }

  public Map<String, CType> getTypes() {
    return types;
  }

  public void putOperation(CType operation) {
    this.operations.put(operation.getName(), operation);
  }

  public Map<String, CType> getOperations() {
    return this.operations;
  }

  public CType lookup(String ref) {
    if (ref.startsWith(ComponentSchemasVisitor.PREFIX)) {
      return this.types.get(ref);
    }

    if (ref.startsWith(ComponentResponsesVisitor.PREFIX)) {
      return this.responses.get(ref);
    }

    throw new IllegalArgumentException("Don't know hwo to lookup ref " + ref);
  }

  public void putResponse(final CType type) {
    this.responses.put(type.getName(), type);
  }

  public Map<String, CType> getResponses() {
    return responses;
  }

  public Set<String> getGeneratedSet() {
    return generatedSet;
  }
}
