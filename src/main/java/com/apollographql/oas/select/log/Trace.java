package com.apollographql.oas.select.log;

import com.apollographql.oas.select.context.Context;

import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

public class Trace {
  private static final Logger logger = Logger.getLogger(Trace.class.getName());

  public static void trace(final Context ctx, final String context, final String message) {
    logger.log(FINE, " ".repeat(ctx.size()) + context + " " + message);
  }

  public static String indent(final Context context) {
    return " ".repeat(context.size());
  }

  public static void warn(final Context ctx, final String context, final String message) {
    logger.log(FINE, " ".repeat(ctx.size()) + context + " " + message);
  }
}
