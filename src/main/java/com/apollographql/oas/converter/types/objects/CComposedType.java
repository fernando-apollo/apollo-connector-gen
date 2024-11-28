package com.apollographql.oas.converter.types.objects;

import com.apollographql.oas.converter.types.CTypeKind;
import io.swagger.v3.oas.models.media.ComposedSchema;
import com.apollographql.oas.converter.types.CType;

public class CComposedType extends CType {
  public CComposedType(String name, ComposedSchema schema) {
    super(name, schema, CTypeKind.COMPOSED, true);
  }

  public void addPropertiesFrom(CType source) {
    if (source.getProps() != null) {
      if (this.props == null) this.props = source.getProps();
      else props.putAll(source.getProps());
    }
  }

}
