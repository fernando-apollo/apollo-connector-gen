package com.apollographql.oas.converter.types.responses;

import com.apollographql.oas.converter.context.DependencySet;
import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

@Deprecated
public class CResponseObjectType extends CType {
  private final String typeRef;

  public CResponseObjectType(String name, String typeRef) {
    super(name, null, CTypeKind.RESPONSE_OBJECT);
    this.typeRef = typeRef;
  }

  public String getTypeRef() {
    return typeRef;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    // do nothing
  }

  @Override
  public Set<CType> getDependencies(Context context) {
    final HashSet<CType> deps = new HashSet<>();
    deps.add(context.lookup(getTypeRef()));
    return deps;
  }

  @Override
  public void select(Context context, Writer writer, DependencySet stack) throws IOException {
    Set<CType> dependencies = getDependencies(context);

    for (CType dependency : dependencies) {
      dependency.select(context, writer, stack);
    }
  }

}
