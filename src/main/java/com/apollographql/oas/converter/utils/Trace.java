package com.apollographql.oas.converter.utils;

import io.swagger.v3.oas.models.media.Schema;

public class Trace {
  public static void printNode(int indent, String ctx, String name, Schema schema) {
    // add as many spaces as the indent says
    System.out.print(" ".repeat(indent * 2));
    System.out.println(String.format(ctx + ": %s - class: %s", name, schema.getClass().getSimpleName()) + ", type: " + schema.getType());
  }

  public static void print(int indent, String ctx, String message) {
    // add as many spaces as the indent says
    System.out.print(" ".repeat(indent * 2));
    System.out.println(String.format("%s: %s", ctx, message));
  }

  public static void warn(int indent, String ctx, String message) {
    // add as many spaces as the indent says
    System.out.print(" ".repeat(indent * 2));
    System.out.println(String.format("%s: [WARN] %s", ctx, message));
  }

}
