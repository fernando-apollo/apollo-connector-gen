package com.apollographql.oas.gen.context;

import com.apollographql.oas.gen.nodes.Scalar;
import com.apollographql.oas.gen.nodes.Type;
import com.apollographql.oas.gen.nodes.props.Prop;

import java.util.*;

public class RefCounter {
  private final Stack<Type> stack;

  public RefCounter(final Context context) {
    this.count = new LinkedHashMap<>();
    this.context = context;
    this.stack = new Stack<Type>();
  }

  private final Context context;

  private final Map<String, Integer> count;

  public Context getContext() {
    return context;
  }

  public void add(Type type) {
    inc(type);
  }

  private void inc(Type type) {
    if (type instanceof Prop || type instanceof Scalar) return;

    final String name = type.getName();
    if (name == null) return;

    Integer value = count.get(name);
    if (value != null) {
      count.put(name, ++value);
    }
    else {
      count.put(name, 1);
    }
  }

  public void count(final Type type) {
    add(type);

//    if (this.stack.contains(type)) {
//      // Circular reference
//      return;
//    }

    this.stack.push(type);

    final Set<Type> dependencies = type.dependencies(getContext());
    for (final Type child : dependencies) {
      count(child);
    }
  }

  public Map<String, Integer> getCount() {
    return this.count;
  }

  public void addAll(final Collection<Type> types) {
    for (Type type : types) {
      count(type);
    }
  }
}
