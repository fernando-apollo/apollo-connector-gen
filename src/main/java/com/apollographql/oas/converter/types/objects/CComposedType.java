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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CComposedType extends CType {
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
    System.out.println(String.format("[composed] -> object: %s", this.getName()));

    writer.append("type ")
      .append(NameUtils.getRefName(getName()))
      .append(" { \n");

    for (Prop prop : this.getProps().values()) {
      System.out.println(String.format("[composed] \t -> property: %s (parent: %s)", prop.getName(), prop.getSource()));
      prop.generate(context, writer);
    }

    writer.append("}\n\n");
  }
}
