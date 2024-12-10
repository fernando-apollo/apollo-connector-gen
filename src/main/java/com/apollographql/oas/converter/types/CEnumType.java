package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.context.DependencySet;
import io.swagger.v3.oas.models.media.Schema;
import joptsimple.internal.Strings;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * @deprecated
 */
public class CEnumType extends CType {
  private final List<String> items;

  public CEnumType(String name, Schema schema, List<String> items) {
    super(name, schema, CTypeKind.ENUM);
    this.items = items;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    String builder = "enum " +
      getSimpleName() +
      " {\n" +
      Strings.join(items.stream().map(s -> " " + s).toList(), ",\n") +
      "\n}\n\n";

    writer.write(builder);
  }

  @Override
  public Set<CType> getDependencies(Context context) {
    return Collections.emptySet();
  }

  @Override
  public void select(Context context, Writer writer, DependencySet stack) throws IOException {
    Set<CType> dependencies = getDependencies(context);

    for (CType dependency : dependencies) {
      dependency.select(context, writer, stack);
    }
  }
}
