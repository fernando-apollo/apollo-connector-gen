package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.context.DependencySet;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

import static com.apollographql.oas.converter.utils.Trace.print;

@Deprecated
public class RefProp extends Prop {
  private final String ref;

  public RefProp(String name, String source, Schema schema, String ref) {
    super(name, source, schema);
    this.ref = ref;
  }

  public String getRef() {
    return ref;
  }

  @Override
  public String getValue(Context context) {
    assert context.lookup(getRef()) != null : "Could not find ref: " + getRef();
    return NameUtils.getRefName(getRef());
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    super.generate(context, writer);
  }

  @Override
  public void select(Context context, Writer writer, DependencySet dependencies) throws IOException {
    final CType lookup = context.lookup(getRef());
    assert lookup != null;

    print(dependencies.size(), getName(), " -> (" + dependencies.peek().getSimpleName() + ")");
    writer
      .append(" ".repeat(dependencies.size()))
      .append(getName());

    if (needsBrackets(lookup)) {
      writer.append(" {");
      writer.append("\n");
    }

    lookup.select(context, writer, dependencies);

    writer
      .append(" ".repeat(dependencies.size()));

    if (needsBrackets(lookup)) {
      writer.append("}");
    }

    writer.append("\n");
    print(dependencies.size(), getName(), " <- (" + dependencies.peek().getSimpleName() + ")");
  }

  @Override
  public String toString() {
    return "PropRef{" +
      "name='" + getName() + '\'' +
      ", ref='" + getRef() + '\'' +
      ", entity='" + getSource() + '\'' +
      '}';
  }

  private boolean needsBrackets(CType lookup) {
    return lookup.getKind() != CTypeKind.ENUM;
  }
}
