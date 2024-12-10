package com.apollographql.oas.select.context;

import com.apollographql.oas.select.nodes.Scalar;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.nodes.props.Prop;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class RefCounter {
  public RefCounter(final Context context) {
    this.count = new LinkedHashMap<>();
    this.context = context;
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

    if (type.getName() == null) return;

    Integer value = count.get(type.getName());
    if (value != null) {
      count.put(type.getName(), ++value);
    }
    else {
      count.put(type.getName(), 1);
    }
  }

  public void count(final Type type) {
    add(type);

    for (final Type child : type.dependencies(getContext())) {
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
