package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

/**
 * @deprecated
 */
public class CArrayType extends CType {
  private static final Logger logger = Logger.getLogger(CArrayType.class.getName());
  private final String itemType;

  public CArrayType(String name, Schema schema, String itemType) {
    super(name, schema, CTypeKind.ARRAY);
    this.itemType = itemType;

    // this should be an array
    this.getProps().put("items", createProp("items", schema));
  }

  public String getItemType() {
    return itemType;
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    logger.log(FINE, String.format("[array] -> type: %s", this.getName()));

    writer.append("type ")
      .append(getSimpleName())
      .append("\n");

    assert getProps().size() == 1 : "Should have only the 'items' property";
    getProps().get("items").generate(context, writer);

    writer.append("}\n\n");
  }
}
