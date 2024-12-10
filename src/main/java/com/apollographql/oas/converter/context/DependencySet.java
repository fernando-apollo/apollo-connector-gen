package com.apollographql.oas.converter.context;

import com.apollographql.oas.converter.types.CType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * @deprecated
 */
public class DependencySet {
  private final Stack<CType> stack;
  private final Map<String, Integer> count;

  public DependencySet() {
     stack = new Stack<>();
     count = new LinkedHashMap<>();
  }

  public boolean contains(CType type) {
    boolean contains = stack.contains(type);
    if (contains) {
      incrementCount(type);
    }
    return contains;
  }

  public void add(CType type) {
    stack.push(type);
    incrementCount(type);
  }

  public CType pop() {
    CType pop = stack.pop();
    decrementCount(pop);
    return pop;
  }

  public CType peek() {
    return stack.peek();
  }

  public Stack<CType> get() {
    return this.stack;
  }

  public void addAll(Set<CType> set) {
    for (CType type : set) {
      this.add(type);
    }
  }

  private void incrementCount(CType type) {
    Integer value = count.get(type.getName());
    if (value != null) {
      count.put(type.getName(), ++value);
    }
    else {
      count.put(type.getName(), 1);
    }
  }

  private void decrementCount(CType type) {
    Integer value = count.get(type.getName());
    if (value != null) {
      count.put(type.getName(), --value);
    }
  }

  public int size() {
    return this.stack.size();
  }

  public int getRefCount(CType type) {
    return this.count.get(type.getName());
  }
}
