package com.apollographql.oas.converter.types.objects;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.gen.TypeGen;
import com.apollographql.oas.converter.types.props.Prop;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.ObjectSchema;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;

import java.io.IOException;
import java.io.Writer;

public class CObjectType extends CType {
  public CObjectType(String name, ObjectSchema schema) {
    super(name, schema, CTypeKind.OBJ, true);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    System.out.println(String.format("[object] -> object: %s", this.getName()));

    writer.append("type ")
      .append(NameUtils.getRefName(getName()))
      .append("\n");

    for (Prop prop : this.getProps().values()) {
      System.out.println(String.format("[object] \t -> property: %s (parent: %s)", prop.getName(), prop.getSource()));
      prop.generate(context, writer);
    }

    writer.append("}\n\n");
  }
}
