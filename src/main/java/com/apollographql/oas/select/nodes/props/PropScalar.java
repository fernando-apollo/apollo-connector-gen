package com.apollographql.oas.select.nodes.props;

import com.apollographql.oas.select.context.Context;
import com.apollographql.oas.select.factory.Factory;
import com.apollographql.oas.select.nodes.Type;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.io.Writer;
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
    return "prop:scalar://" + getName();
  }

  @Override
  public void visit(final Context context) {
    context.enter(this);
//    trace(context, "-> [prop-scalar]", "in " + getName() + ", type: " + getType());

    if (this.propType == null) {
      this.propType = Factory.fromSchema(this, getSchema());
      this.propType.visit(context);
      setVisited(true);
    }

//    trace(context, "<- [prop-scalar]", "out " + getName() + ", type: " + getType());
    context.leave();
  }

  @Override
  public String getValue(Context context) {
    return getType();
  }

  @Override
  public void select(final Context context, final Writer writer) throws IOException {
    super.select(context, writer);
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
