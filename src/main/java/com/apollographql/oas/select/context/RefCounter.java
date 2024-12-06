package com.apollographql.oas.select.context;

import com.apollographql.oas.select.nodes.Type;
import com.apollographql.oas.select.nodes.props.Prop;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RefCounter {
  private final Map<String, Integer> count;

  public RefCounter() {
     count = new LinkedHashMap<>();
  }

  public void add(Type type) {
    inc(type);
  }

  private void inc(Type type) {
    Integer value = count.get(type.id());
    if (value != null) {
      System.out.println("[inc] " + getTree(type));
      count.put(type.id(), ++value);
    }
    else {
      count.put(type.id(), 1);
    }
  }

  public void count(final Type type) {
    add(type);

    for (final Type child : type.getChildren()) {
      count(child);
    }
  }

  public Map<String, Integer> get() {
    return this.count;
  }

  String getTree(Type type) {
    StringBuilder builder = new StringBuilder();
    Type current = type;
    do {
      builder.append(" <- " + current.id() + " (" + current.getClass().getSimpleName() + ")");
    }
    while ((current = current.getParent()) != null);

    return builder.toString();
  }

  public void addAll(final Collection<Type> types) {
    for (Type type : types) {
      count(type);
    }
  }
}
