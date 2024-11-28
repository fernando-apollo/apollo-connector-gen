package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import io.swagger.v3.oas.models.media.Schema;

public class ArrayProp extends Prop {
  private final Prop items;

  public ArrayProp(String name, String source, Schema schema, Prop items) {
    super(name, source, schema);
    this.items = items;
  }

  public Prop getItems() {
    return items;
  }

  @Override
  public String getValue(Context context) {
//    assert context.lookup(itemsSchema.get$ref()) != null : "Could not find items ref: " + itemsSchema.get$ref();
    return "[" + getItems().getValue(context) + "]";
  }
}
