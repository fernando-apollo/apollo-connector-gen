package com.apollographql.oas.gen.context;

import com.apollographql.oas.converter.utils.NameUtils;
import com.apollographql.oas.gen.nodes.Type;
import com.apollographql.oas.gen.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.*;

import static com.apollographql.oas.gen.log.Trace.warn;
import static com.apollographql.oas.gen.log.Trace.trace;

public class Context {
  private static final String COMPONENTS_SCHEMAS = "#/components/schemas/";
  public static final String COMPONENTS_RESPONSES = "#/components/responses/";

  private final OpenAPI parser;
  private final Map<String, Type> types = new TreeMap<>();

  private final Set<String> generatedSet = new LinkedHashSet<>();

  private final Stack<Type> stack = new Stack<>();
  private Prompt prompt;

  public Context(final OpenAPI parser, final Prompt prompt) {
    this.parser = parser;
    this.prompt = prompt;
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
      warn(this, "[context]", "Possible recursion? We have entered this type more than once! " + Type.getRootPathFor(type));
    }
    this.stack.push(type);
  }

  public void leave() {
    this.stack.pop();
  }

  public int size() {
    return this.stack.size();
  }

  public Prompt getPrompt() {
    return prompt;
  }

  public void setPrompt(final Prompt prompt) {
    this.prompt = prompt;
  }

  public Schema<?> lookupRef(final String ref) {
    if (ref.startsWith(COMPONENTS_SCHEMAS)) {
      return parser.getComponents().getSchemas().get(NameUtils.getRefName(ref));
    }

    return null;
  }

  public ApiResponse lookupResponse(final String ref) {
    if (ref.startsWith(COMPONENTS_RESPONSES)) {
      return parser.getComponents().getResponses().get(NameUtils.getRefName(ref));
    }

    return null;
  }

  public void store(final String name, final Type type) {
    trace(this, "[context::store]", "store " + type.id());
    this.types.put(name, type);
  }

  public Type get(final String name) {
    if (this.types.containsKey(name)) {
      trace(this, " [context::inc]", "cached => " + name);
    }

    return this.types.get(name);
  }

  public boolean inContextOf(final Class<?> clazz, final Type type) {
    return getStack().stream()
      .anyMatch(t -> t != type && t.getClass().isAssignableFrom(clazz));
  }

}
