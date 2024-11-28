package com.apollographql.oas.converter.types.responses;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;

import java.io.IOException;
import java.io.Writer;

public class CResponseArrayType extends CType {
  private final String itemsRef;

  public CResponseArrayType(String name, String itemsRef) {
    super(name, null, CTypeKind.RESPONSE_ARRAY);
    this.itemsRef = itemsRef;
  }

  public String getItemsRef() {
    return itemsRef;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    // do not generate anything for this ?
  }
}
