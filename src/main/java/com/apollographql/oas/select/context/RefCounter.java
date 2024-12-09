package com.apollographql.oas.select.context;

import com.apollographql.oas.select.nodes.Scalar;
import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.nodes.props.Prop;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class RefCounter {
  private final Map<String, Integer> count;

  public RefCounter() {
     count = new LinkedHashMap<>();
  }

  public void add(Type type) {
    inc(type);
  }

  private void inc(Type type) {
    if (type instanceof Prop || type instanceof Scalar) return;

    Integer value = count.get(type.id());
    if (value != null) {
//      System.out.println("[inc] " + type.id());
      count.put(type.id(), ++value);
    }
    else {
//      System.out.println("[put] " + type.id());
      count.put(type.id(), 1);
    }
  }

  public void count(final Type type) {
    add(type);

    for (final Type child : type.dependencies()) {
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
