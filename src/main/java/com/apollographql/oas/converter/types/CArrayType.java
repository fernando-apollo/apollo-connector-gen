package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.objects.Prop;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;

public class CArrayType extends CType {
  private final String itemType;

  public CArrayType(String name, Schema schema, String itemType) {
    super(name, schema, CTypeKind.ARRAY, true);
    this.itemType = itemType;

    final Prop prop = new Prop(getName(), "items", "array", schema);

    // this should be an array
    this.getProps().put("items", prop);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    super.generate(context, writer);
//    final TypeGen typeGen = new TypeGen(this, context);
//
//    Schema schema = getSchema();
//    if (schema.getType().equals("array")) {
//      final Schema itemsSchema = schema.getItems();
//
//      if (itemsSchema != null) {
//        System.out.println("CArrayType.generate array with items " + itemsSchema.getClass().getSimpleName());
//        final String ref = itemsSchema.get$ref();
//
//        if (itemsSchema.getType() == null && ref != null) {
//          final CType itemType = context.getType(ref);
//          Prop prop = new Prop(getName(), "items", itemsSchema);
//
//          if (itemType != null) {
//            typeGen.addField("items", String.format("[ %s ]", itemType.getName()), itemsSchema.getDescription(), "array of items");
//          }
//          else {
//            throw new IllegalStateException(String.format("Could not find type %s", ref));
//          }
//        }
//        else if (itemsSchema.getType() != null) {
//          typeGen.addScalarArray("items", schema.getType());
//        }
//        else throw new IllegalStateException("What is the type for this?" + "items" + ", schema:" + schema);
//      }
//    }
//
//    typeGen.end();
//    writer.write(typeGen.toString());
  }
}
