package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.props.ScalarProp;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

public class CArrayType extends CType {
  private final String itemType;

  public CArrayType(String name, Schema schema, String itemType) {
    super(name, schema, CTypeKind.ARRAY, true);
    this.itemType = itemType;

    // this should be an array
    this.getProps().put("items", new ScalarProp("items", getName(), "array", schema));
  }

  public String getItemType() {
    return itemType;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    super.generate(context, writer);
  }
}
