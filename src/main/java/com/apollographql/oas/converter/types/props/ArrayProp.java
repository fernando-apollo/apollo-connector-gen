package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

public class ArrayProp extends Prop {
  private final Prop items;

  public ArrayProp(String name, String source, Schema schema, Prop items) {
    super(name, source, schema);
    this.items = items;

    if (items == null) {
      throw new IllegalArgumentException("Items type cannot be null for array!");
    }
  }

  public Prop getItems() {
    return items;
  }

  @Override
  public String getValue(Context context) {
//    assert context.lookup(itemsSchema.get$ref()) != null : "Could not find items ref: " + itemsSchema.get$ref();
    return "[" + getItems().getValue(context) + "]";
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    super.generate(context, writer);
  }
}
