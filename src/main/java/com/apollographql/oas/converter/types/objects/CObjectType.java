package com.apollographql.oas.converter.types.objects;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.types.props.Prop;
import com.apollographql.oas.converter.types.props.RefProp;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.ObjectSchema;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import static com.apollographql.oas.converter.types.objects.CComposedType.needsBrackets;
import static com.apollographql.oas.converter.utils.Trace.print;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

public class CObjectType extends CType {
  private static final Logger logger = Logger.getLogger(CObjectType.class.getName());

  public CObjectType(String name, ObjectSchema schema) {
    super(name, schema, CTypeKind.OBJ);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    logger.log(FINE, String.format("[object] -> object: %s", this.getName()));

    writer.append("type ")
      .append(NameUtils.getRefName(getName()))
      .append(" {\n");

    for (Prop prop : this.getProps().values()) {
      logger.log(FINE, String.format("[object] \t -> property: %s (parent: %s)", prop.getName(), prop.getSource()));
      prop.generate(context, writer);
    }

    writer.append("}\n\n");
  }

  @Override
  public void select(Context context, Writer writer, Stack<CType> stack) throws IOException {
    if (stack.contains(this)) {
      logger.log(WARNING, "Possible recursion! Stack should not already contain " + this);
    }
    else {
      stack.push(this);
      int indent = stack.size();

      for (final Prop prop : this.getProps().values()) {
        prop.select(context, writer, stack);
      }

      stack.pop();
    }
  }
}
