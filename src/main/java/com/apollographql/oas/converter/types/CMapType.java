package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.objects.Prop;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

public class CMapType extends CType {
  public CMapType(String name, Schema schema, String key, String type) {
    super(name, schema, CTypeKind.MAP, true);

    getProps().put("key", new Prop(name, key, "map", null));
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    //
    System.out.println("CMapType.generate: " + getName() + ", props: " + this.props);
  }
}
