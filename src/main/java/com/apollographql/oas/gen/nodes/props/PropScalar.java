package com.apollographql.oas.gen.nodes.props;

import com.apollographql.oas.gen.context.Context;
import com.apollographql.oas.gen.factory.Factory;
import com.apollographql.oas.gen.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Collections;
import java.util.Set;

public class PropScalar extends Prop {
  protected final String type;
  private Type propType;

  public PropScalar(final Type parent, final String name, final String type, final Schema schema) {
    super(parent, name, schema);
    this.type = type;
  }

  @Override
  public String id() {
    return "prop:scalar:" + getName();
  }

  @Override
  public void visit(final Context context) {
    if (isVisited()) return;

    context.enter(this);
    if (this.propType == null) {
      this.propType = Factory.fromSchema(this, getSchema());
      this.propType.visit(context);
      setVisited(true);
    }

    context.leave(this);
  }

  @Override
  public String getValue(Context context) {
    return getType();
  }

  @Override
  public String toString() {
    return "PropScalar {" +
      "name='" + getName() + '\'' +
      ", type='" + getType() + '\'' +
      ", parent='" + getParent() + '\'' +
      '}';
  }

  public String getType() {
    return type;
  }

  public String forPrompt(final Context context) {
    return getName() + ": " + getValue(context);
  }

  @Override
  public Set<Type> dependencies(final Context context) {
    return Collections.emptySet();
  }
}
