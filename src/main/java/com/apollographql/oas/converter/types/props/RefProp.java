package com.apollographql.oas.converter.types.props;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.utils.NameUtils;
import io.swagger.v3.oas.models.media.Schema;

public class RefProp extends Prop {
  private final String ref;

  public RefProp(String name, String source, Schema schema, String ref) {
    super(name, source, schema);
    this.ref = ref;
  }

  public String getRef() {
    return ref;
  }

  @Override
  public String getValue(Context context) {
    assert context.lookup(getRef()) != null : "Could not find ref: " + getRef();
    return NameUtils.getRefName(getRef());
  }
}
