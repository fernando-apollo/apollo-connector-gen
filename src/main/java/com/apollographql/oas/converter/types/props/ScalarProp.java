package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Stack;

import static com.apollographql.oas.converter.utils.Trace.print;

public class ScalarProp extends Prop {

  protected final String type;

  public ScalarProp(final String name, final String source, final String type, final Schema schema) {
    super(name, source, schema);
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScalarProp prop = (ScalarProp) o;
    return Objects.equals(getName(), prop.getName()) && Objects.equals(getSource(), prop.getSource());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getSource());
  }

  @Override
  public String toString() {
    return "Prop{" +
      "name='" + getName() + '\'' +
      ", type='" + getType() + '\'' +
      ", entity='" + getSource() + '\'' +
      '}';
  }

  public String getType() {
    return type;
  }

  @Override
  public String getValue(Context context) {
    return getType();
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException  {
    super.generate(context, writer);
  }

  @Override
  public void select(Context context, Writer writer, Stack<CType> stack) throws IOException {
    print(stack.size(), getName(), " -> (" + stack.peek().getSimpleName() + ")");

    final String fieldName = getName().startsWith("@") ? getName().substring(1) : getName();
    writer
      .append(" ".repeat(stack.size()))
      .append(fieldName)
      .append("\n");
  }
}
