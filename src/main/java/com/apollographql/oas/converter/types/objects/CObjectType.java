package com.apollographql.oas.converter.types.objects;

import io.swagger.v3.oas.models.media.ObjectSchema;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.types.CTypeKind;

public class CObjectType extends CType {
  public CObjectType(String name, ObjectSchema schema) {
    super(name, schema, CTypeKind.OBJ, true);
  }
}
