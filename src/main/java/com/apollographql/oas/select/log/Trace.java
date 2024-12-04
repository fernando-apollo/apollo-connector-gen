package com.apollographql.oas.select.log;

import com.apollographql.oas.select.context.Context;

public class Trace {
  public static void trace(final Context ctx, final String context, final String message) {
    System.out.println(" ".repeat(ctx.size()) + context + " " + message);
  }

  public static String indent(final Context context) {
    return " ".repeat(context.size());
  }

  public static void warn(final Context ctx, final String context, final String message) {
    System.err.println(" ".repeat(ctx.size()) + context + " " + message);
  }
}
