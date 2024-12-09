package com.apollographql.oas.select.log;

import com.apollographql.oas.select.context.Context;

import java.util.logging.Logger;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

public class Trace {
  private static final Logger logger = Logger.getLogger(Trace.class.getName());

  public static void trace(final Context ctx, final String context, final String message) {
    final int count = ctx != null ? ctx.size() : 0;
    logger.log(FINE, " ".repeat(count) + ("(" + count + ")") + context + " " + message);
  }

  public static void print(final Context ctx, final String message) {
    System.out.println(" ".repeat(ctx != null ? ctx.size() : 0) + " " + message);
  }

  public static String indent(final Context ctx) {
    return " ".repeat(ctx != null ? ctx.size() : 0);
  }

  public static void warn(final Context ctx, final String context, final String message) {
//    logger.log(WARNING, " ".repeat(ctx != null ? ctx.size() : 0) + context + " " + message);
    logger.log(WARNING, context + " " + message);
  }
}
