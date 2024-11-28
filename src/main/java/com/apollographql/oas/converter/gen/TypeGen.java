package com.apollographql.oas.converter.gen;

import com.apollographql.oas.converter.types.props.Prop;
import com.apollographql.oas.converter.utils.GqlUtils;
import io.swagger.v3.oas.models.media.Schema;
import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;
import com.apollographql.oas.converter.utils.NameUtils;

public class TypeGen {
  private final StringBuilder builder = new StringBuilder();
  private final Context context;
  private final CType type;

  public TypeGen(CType type, Context context) {
    this.type = type;
    this.context = context;
    builder.append("type ")
      .append(NameUtils.getRefName(type.getName()))
      .append(" { # ")
      .append(type.getKind())
      .append("\n");
  }

  public CType getType() {
    return type;
  }

  /**
   * string	(none)	String	A UTF-8 encoded string.
   * string	date	String	ISO 8601 date string (e.g., YYYY-MM-DD).
   * string	date-time	String	ISO 8601 date-time string (e.g., YYYY-MM-DDTHH:mm:ssZ).
   * string	binary	String or Custom	Typically mapped to String or a custom scalar.
   * string	byte	String	Base64-encoded string.
   * integer	(none)	Int	A 32-bit signed integer.
   * integer	int32	Int	A 32-bit signed integer.
   * integer	int64	String or BigInt	Typically a string or a custom scalar in GraphQL to handle large integers.
   * number	(none)	Float	A floating-point number.
   * number	float	Float	A single-precision 32-bit floating-point number.
   * number	double	Float or Custom	A double-precision 64-bit floating-point number.
   * boolean	(none)	Boolean	A true or false value.
   */
  public void addScalar(final Prop prop, Schema schema, String type) {
    String description = schema.getDescription();
    String entity = prop.getSource();

    switch (type) {
      case "string", "date", "date-time" -> this.addField(prop, "String", description, entity);
      case "integer" -> {
        if (schema.getFormat() != null && schema.getFormat().equals("int64")) {
          // this.addField(prop, "String", description, entity);
          // this is wrong, unfortunately
          this.addField(prop, "Int", description, entity);
        } else {
          this.addField(prop, "Int", description, entity);
        }
      }
      case "number" -> {
        this.addField(prop, "Float", description, entity);
      }
      case "boolean" -> {
        this.addField(prop, "Boolean", description, entity);
      }
      case "object" -> {
        this.addField(prop, "JSON", description, entity);
      }
      default -> {
        throw new IllegalStateException("[addScalar] Cannot generate type = " + prop + ", type: " + type);
      }
    }
  }

  public void addField(Prop prop, String type, String description, String source) {
    if (description != null) {
      if (description.contains("\n") || description.contains("\r") || description.contains("\"")) {
         builder.append("  \"\"\"\n").append("  ").append(description).append("\n  \"\"\"\n");
      }
      else {
        builder.append("  \"").append(description).append("\"\n");
      }
    }

    // some fields start with '@' like '@type' -- need to remove the '@' for GQL
    final String fieldName = prop.getName().startsWith("@") ? prop.getName().substring(1) : prop.getName();
    builder.append("  ").append(fieldName).append(": ");//.append(NameUtils.getRefName(type));

    this.getPropValue(prop, type);

    if (prop.isRequired())
      builder.append("!");

    if (source != null)
      builder.append(" # ").append(NameUtils.getRefName(source));

    builder.append("\n");
  }

  private void getPropValue(final Prop prop, String type) {
    builder.append(prop.getValue(context));
  }

  public void addScalarArray(String name, Schema itemsSchema) {
    builder.append("  ").append(name).append(": [");
    builder.append(GqlUtils.getGQLScalarType(itemsSchema));
    builder.append("]").append("\n");
  }

  @Override
  public String toString() {
    return builder.toString();
  }

  public void end() {
    builder.append("}\n\n");
  }

}
