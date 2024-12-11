package com.apollographql.oas.converter.types.responses;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.context.DependencySet;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

@Deprecated
public class CResponseArrayType extends CType {
  private static final Logger logger = Logger.getLogger(CResponseArrayType.class.getName());

  private final String itemsRef;

  public CResponseArrayType(String name, String itemsRef) {
    super(name, null, CTypeKind.RESPONSE_ARRAY);
    this.itemsRef = itemsRef;
  }

  public String getItemsRef() {
    return itemsRef;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    // do not generate anything for this ?
  }

  @Override
  public Set<CType> getDependencies(Context context) {
    final Set<CType> deps = new HashSet<>();

    final CType lookup = context.lookup(getItemsRef());
    if (lookup != null)
      deps.add(lookup);

    return deps;
  }

  @Override
  public void select(Context context, Writer writer, DependencySet stack) throws IOException {
    if (stack.contains(this)) {
      logger.log(WARNING, "Possible recursion! Stack should not already contain " + this);
    }
    else {
      stack.add(this);

      Set<CType> dependencies = getDependencies(context);

      for (CType dependency : dependencies) {
        dependency.select(context, writer, stack);
      }

      stack.pop();
    }
  }
}
