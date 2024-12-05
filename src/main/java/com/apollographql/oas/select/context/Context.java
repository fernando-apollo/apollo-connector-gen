package com.apollographql.oas.select.context;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.nodes.Obj;
import com.apollographql.oas.select.nodes.Type;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.*;

public class Context {
  private static final String COMPONENTS_SCHEMAS = "#/components/schemas/";
  public static final String COMPONENTS_RESPONSES = "#/components/responses/";

  private final OpenAPI parser;
  private Map<String, Type> types = new TreeMap<>();

//  private Map<String, Type> operations = new TreeMap<>();
//
//  private Map<String, Type> responses = new TreeMap<>();
  private Set<String> generatedSet = new LinkedHashSet<>();

  private final Stack<Type> stack = new Stack<>();

  public Context(final OpenAPI parser) {
    this.parser = parser;
  }

  public Map<String, Type> getTypes() {
    return types;
  }

  public Stack<Type> getStack() {
    return stack;
  }

  //  public static boolean isResponseType(String type) {
//    return type.startsWith(ComponentResponsesVisitor.PREFIX);
//  }
//
//  public static boolean isSchemaType(String type) {
//    return type.startsWith(ComponentSchemasVisitor.PREFIX);
//  }
//
//  public Type putType(String name, Type value) {
//    types.put(name, value);
//    return value;
//  }
//
//  @Override
//  public String toString() {
//    return types.toString();
//  }
//
//  public boolean containsType(String name) {
//    return types.containsKey(name);
//  }
//
//  public Type getType(String name) {
//    return types.get(name);
//  }
//
//  public Map<String, Type> getTypes() {
//    return types;
//  }
//
//  public void putOperation(Type operation) {
//    this.operations.put(operation.getName(), operation);
//  }
//
//  public Map<String, Type> getOperations() {
//    return this.operations;
//  }
//
//  public Type lookup(String ref) {
//    if (ref.startsWith(ComponentSchemasVisitor.PREFIX)) {
//      return this.types.get(ref);
//    }
//
//    if (ref.startsWith(ComponentResponsesVisitor.PREFIX)) {
//      return this.responses.get(ref);
//    }
//
//    throw new IllegalArgumentException("Don't know hwo to lookup ref " + ref);
//  }
//
//  public void putResponse(final Type type) {
//    this.responses.put(type.getName(), type);
//  }
//
//  public Map<String, Type> getResponses() {
//    return responses;
//  }
//
  public Set<String> getGeneratedSet() {
    return generatedSet;
  }

  public void enter(final Type type) {
    this.stack.push(type);
  }

  public void leave(final Type type) {
    this.stack.pop();
  }

  public int size() {
    return this.stack.size();
  }

  public Schema lookupRef(final String ref) {
    if (ref.startsWith(COMPONENTS_SCHEMAS)) {
      return parser.getComponents().getSchemas().get(NameUtils.getRefName(ref));
    }

    return null;
  }

  public ApiResponse lookupResponse(final String ref) {
    if (ref.startsWith(COMPONENTS_RESPONSES)) {
      final ApiResponse response = parser.getComponents().getResponses().get(NameUtils.getRefName(ref));
      return response;
    }

    return null;
  }

  public void store(final String name, final Type type) {
    this.types.put(name, type);
  }

  public Type get(final String name) {
    return this.types.get(name);
  }
}
