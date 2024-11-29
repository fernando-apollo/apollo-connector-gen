package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import static com.apollographql.oas.converter.utils.Trace.print;

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
  public void select(Context context, Writer writer, Stack<CType> stack) throws IOException {
    final CType lookup = context.lookup(getRef());
    assert lookup != null;

    print(stack.size(), getName(), " -> (" + stack.peek().getSimpleName() + ")");
    writer
      .append(" ".repeat(stack.size()))
      .append(getName())
      .append(" {\n");

    lookup.select(context, writer, stack);

    writer
      .append(" ".repeat(stack.size()))
      .append("}\n");

    print(stack.size(), getName(), " <- (" + stack.peek().getSimpleName() + ")");
  }
}
