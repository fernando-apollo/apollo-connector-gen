package com.apollographql.oas.converter.types.objects;

import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.types.props.Prop;
import io.swagger.v3.oas.models.media.ComposedSchema;
import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.utils.NameUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CUnionType extends CType {
  private List<String> types = new ArrayList<>();

  public CUnionType(String name, ComposedSchema schema) {
    super(name, schema, CTypeKind.UNION);
  }

  public void addType(String name) {
    types.add(name);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    // when we generate this Union in GQL it will be something like
    // union MyUnion = Type1 | Type2 | Type3 -> name + "=" + types.join(' | ')
    // and then we pray that the types are defined somewhere else
    final StringBuilder builder = new StringBuilder();
    builder.append("union ").append(NameUtils.getRefName(getName())).append(" = ");
    builder.append(String.join(" | ", types.stream().map(NameUtils::getRefName).toList()));
    builder.append("\n\n");

    writer.write(builder.toString());
  }

  public List<String> getTypes() {
    return types;
  }

  @Override
  public Set<CType> getDependencies(Context context) {
    final Set<CType> deps = new HashSet<>();

    for (String ref : getTypes()) {
      final CType type = context.lookup(ref);
      if (type != null) deps.add(type);
    }

    return deps;
  }
}
