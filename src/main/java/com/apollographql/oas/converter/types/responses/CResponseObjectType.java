package com.apollographql.oas.converter.types.responses;

import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;

import java.io.IOException;
import java.io.Writer;

public class CResponseObjectType extends CType {
  private final String typeRef;

  public CResponseObjectType(String name, String typeRef) {
    super(name, null, CTypeKind.RESPONSE_OBJECT, true);
    this.typeRef = typeRef;
  }

  public String getTypeRef() {
    return typeRef;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    // do nothing
//    super.generate(context, writer);
//    writer.write(getTypeRef());
  }
}
