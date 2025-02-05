package com.apollographql.oas.gen.naming;

public abstract class AbstractConverter implements Converter {
  protected Converter next;

  public AbstractConverter(Converter next) {
    this.next = next;
  }

  protected String processNext(String input) {
    return (next != null) ? next.convert(input) : input;
  }
}
