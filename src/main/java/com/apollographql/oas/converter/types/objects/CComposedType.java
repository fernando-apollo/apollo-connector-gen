package com.apollographql.oas.converter.types.objects;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.types.props.ArrayProp;
import com.apollographql.oas.converter.types.props.Prop;
import com.apollographql.oas.converter.types.props.RefProp;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.ComposedSchema;
import com.apollographql.oas.converter.types.CType;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;
import java.util.logging.Logger;

import static com.apollographql.oas.converter.utils.Trace.print;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

public class CComposedType extends CType {
  private static final Logger logger = Logger.getLogger(CComposedType.class.getName());
  
  public CComposedType(String name, ComposedSchema schema) {
    super(name, schema, CTypeKind.COMPOSED);
  }

  public void addPropertiesFrom(CType source) {
    if (source.getProps() != null) {
      if (this.props == null) this.props = source.getProps();
      else props.putAll(source.getProps());
    }
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    logger.log(FINE, String.format("[composed] -> object: %s", this.getName()));

    writer.append("type ")
      .append(getSimpleName())
      .append(" { \n");

    for (Prop prop : this.getProps().values()) {
      logger.log(FINE, String.format("[composed] \t -> property: %s (parent: %s)", prop.getName(), prop.getSource()));
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
      for (final Prop prop : this.getProps().values()) {
//      if (needsBrackets(prop)) {
//        print(indent, getSimpleName(), prop.getName() + " {");
//        writer.append(" ".repeat(indent)).append(prop.getName()).append(" {");
//      }

        // generate selection
        prop.select(context, writer, stack);

//      if (needsBrackets(prop)) {
//        print(indent, getSimpleName(), "}");
//        writer.append(" ".repeat(indent)).append("}");
//      }

//        writer.append("\n");
      }
      stack.pop();
    }
  }

  static boolean needsBrackets(Prop prop) {
    return prop instanceof RefProp || prop instanceof ArrayProp;
  }
}
