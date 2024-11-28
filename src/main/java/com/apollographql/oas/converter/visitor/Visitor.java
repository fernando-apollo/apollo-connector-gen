package com.apollographql.oas.converter.visitor;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.utils.Trace;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.converter.utils.Trace.print;
import static com.apollographql.oas.converter.utils.Trace.warn;

public abstract class Visitor {
  protected final Context context;
  protected int indent = 0;

  public Visitor(final Context context) {
    this.context = context;
  }

  public abstract void visit();
  public abstract void generate(Writer writer) throws IOException;

  protected CType lookupRef(String schemaRef) {
    print(indent, "  [" + "lookupRef" + "]", "looking for schemaRef: " + schemaRef);

    // let's look it up
    final CType lookup = context.lookup(schemaRef);

    if (lookup != null) {
      print(indent, "  [" + "lookupRef" + "]", "found type: '" + lookup + "' in context ");
      return lookup;
    } else {
      warn(indent, "  [" + "lookupRef" + "]", "should have found type: '" + schemaRef + "' in context ");
    }
    return null;
  }

  abstract protected CType store(CType type);
}
