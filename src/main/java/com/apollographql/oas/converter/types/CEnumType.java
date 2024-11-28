package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.Schema;
import joptsimple.internal.Strings;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CEnumType extends CType {
  private final List<String> items;

  public CEnumType(String name, Schema schema, List<String> items) {
    super(name, schema, CTypeKind.ENUM);
    this.items = items;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    final StringBuilder builder = new StringBuilder();
    builder.append("enum ");
    builder.append(NameUtils.getRefName(this.getName()));
    builder.append(" {\n");
    builder.append(Strings.join(items.stream().map(s -> " " + s).toList(), ",\n"));
    builder.append("\n}\n\n");

    writer.write(builder.toString());
  }

  @Override
  public Set<CType> getDependencies(Context context) {
    return Collections.emptySet();
  }
}
