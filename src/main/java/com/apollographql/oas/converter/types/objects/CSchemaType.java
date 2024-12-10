package com.apollographql.oas.converter.types.objects;

import com.apollographql.oas.converter.types.CTypeKind;
import io.swagger.v3.oas.models.media.Schema;
import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;

import java.io.IOException;
import java.io.Writer;

/**
 * @deprecated
 */
public class CSchemaType extends CType {
  public CSchemaType(Schema schema) {
    super(null, schema, CTypeKind.OBJ_ANON);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    // do nothing for now
  }
}
