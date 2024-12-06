package com.apollographql.oas.select.context;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.select.nodes.Scalar;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.nodes.props.Prop;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.*;

import static com.apollographql.oas.select.log.Trace.warn;
import static com.apollographql.oas.select.log.Trace.trace;

public class Context {
  private static final String COMPONENTS_SCHEMAS = "#/components/schemas/";
  public static final String COMPONENTS_RESPONSES = "#/components/responses/";

  private final OpenAPI parser;
  private Map<String, Type> types = new TreeMap<>();

  private final Map<String, Integer> refCounter = new LinkedHashMap<>();

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

  public Set<String> getGeneratedSet() {
    return generatedSet;
  }

  public void enter(final Type type) {
    if (this.stack.contains((type))) {
      warn(this, "[context]", "Possible recursion? We have entered this type more than once! " + type.id());
    }

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
    trace(this, "[context::store]", "store " + type.id());
    this.types.put(name, type);
    inc(type);
  }

  public Type get(final String name) {
    return this.types.get(name);
  }

  private void inc(Type type) {
    if (type instanceof Prop || type instanceof Scalar) return;

    Integer value = refCounter.get(type.id());
    if (value != null) {
      trace(this, "[context::inc]", "inc: " + type.id());
      refCounter.put(type.id(), ++value);
    }
    else {
      trace(this, "[context::inc]","add: " + type.id());
      refCounter.put(type.id(), 1);
    }
  }
}
