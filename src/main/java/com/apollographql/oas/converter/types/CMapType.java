package com.apollographql.oas.converter.types;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.operations.COperationType;
import com.apollographql.oas.converter.types.props.ScalarProp;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

public class CMapType extends CType {
  private static final Logger logger = Logger.getLogger(CMapType.class.getName());
  public CMapType(String name, Schema schema, String key, String type) {
    super(name, schema, CTypeKind.MAP);

    getProps().put("key", new ScalarProp(name, key, "map", null));
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    //
    logger.log(WARNING, "CMapType.generate: " + getName() + ", props: " + this.props);
  }
}
