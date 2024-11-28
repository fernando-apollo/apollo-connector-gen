package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

public class CArrayType extends CType {
  private final String itemType;

  public CArrayType(String name, Schema schema, String itemType) {
    super(name, schema, CTypeKind.ARRAY);
    this.itemType = itemType;

    // this should be an array
    this.getProps().put("items", createProp("items", schema));
  }

  public String getItemType() {
    return itemType;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    System.out.println(String.format("[array] -> type: %s", this.getName()));
    writer.append("type ")
      .append(NameUtils.getRefName(getName()))
      .append("\n");

    assert getProps().size() == 1 : "Should have only the 'items' property";
    getProps().get("items").generate(context, writer);

    writer.append("}\n\n");
  }
}
