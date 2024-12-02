package com.apollographql.oas.converter.utils;

import io.swagger.v3.oas.models.media.Schema;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

public class Trace {
  private static final Logger logger = Logger.getLogger(Trace.class.getName());
  public static void printNode(int indent, String ctx, String name, Schema schema) {

    // add as many spaces as the indent says
    logger.log(FINE, spacing(indent) +
      String.format(ctx + ": %s - class: %s",
        name, schema.getClass().getSimpleName()) + ", type: " + schema.getType());
  }

  public static void print(int indent, String ctx, String message) {
    logger.log(FINE, spacing(indent) + String.format("%s: %s", ctx, message));
  }

  public static void warn(int indent, String ctx, String message) {
    // add as many spaces as the indent says
    logger.log(WARNING, spacing(indent) + String.format("%s: [WARN] %s", ctx, message));
  }

  private static String spacing(int indent) {
    return " ".repeat(indent * 2);
  }
}
