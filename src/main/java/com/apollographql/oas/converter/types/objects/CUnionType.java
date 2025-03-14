package com.apollographql.oas.converter.types.objects;

import com.apollographql.oas.converter.context.DependencySet;
import com.apollographql.oas.converter.types.CTypeKind;
import com.apollographql.oas.converter.types.props.Prop;
import com.apollographql.oas.gen.naming.Naming;
import io.swagger.v3.oas.models.media.ComposedSchema;
import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.types.CType;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

@Deprecated
public class CUnionType extends CType {
  private static final Logger logger = Logger.getLogger(CUnionType.class.getName());

  private final Set<String> types = new LinkedHashSet<>();

  public CUnionType(String name, ComposedSchema schema) {
    super(name, schema, CTypeKind.UNION);
  }

  public void addType(String name) {
    types.add(name);
  }

  @Override
  public void generate(Context context, Writer writer) throws IOException {
    // when we generate this Union in GQL it will be something like
    // union MyUnion = Type1 | Type2 | Type3 -> name + "=" + types.join(' | ')
    // and then we pray that the types are defined somewhere else
    writer.append("#### NOT SUPPORTED YET BY CONNECTORS!!! union ").append(getSimpleName()).append(" = ");
    writer.append(String.join("# | ", types.stream().map(Naming::getRefName).toList()));
    writer.append("#\n\n");

    logger.log(FINE, String.format("[union] -> object: %s", this.getName()));

    writer.append("type ")
      .append(getSimpleName())
      .append(" { #### replacement for Union ")
      .append(getSimpleName())
      .append("\n");

    Set<String> generatedSet = new LinkedHashSet<>();

    for (String type : types) {
      CType lookup = context.lookup(type);
      assert lookup != null;

      for (Prop prop : lookup.getProps().values().stream().filter(p -> !generatedSet.contains(p.getName())).toList()) {
        logger.log(FINE, String.format("[union] \t -> property: %s (parent: %s)", prop.getName(), prop.getSource()));
        prop.generate(context, writer);

        generatedSet.add(prop.getName());
      }
    }

    writer.append("} ### End replacement for ")
      .append(getSimpleName())
      .append("\n\n");
//    writer.write(writer.toString());
  }

  public Set<String> getTypes() {
    return types;
  }

  @Override
  public Set<CType> getDependencies(Context context) {
    final Set<CType> deps = new HashSet<>();

    for (String ref : getTypes()) {
      final CType type = context.lookup(ref);
      if (type != null) deps.add(type);
    }

    return deps;
  }

  @Override
  public Set<CType> getSkipSet(Context context) {
    return getDependencies(context);
//    return Collections.emptySet();
  }

  @Override
  public void select(Context context, Writer writer, DependencySet stack) throws IOException {
    Set<CType> dependencies = getDependencies(context);

    for (CType dependency : dependencies) {
      dependency.select(context, writer, stack);
    }
  }
}
