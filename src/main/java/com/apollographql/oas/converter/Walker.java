package com.apollographql.oas.converter;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.visitor.ComponentResponsesVisitor;
import com.apollographql.oas.converter.visitor.ComponentSchemasVisitor;
import com.apollographql.oas.converter.visitor.PathsVisitor;
import com.apollographql.oas.converter.visitor.Visitor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Deprecated
public class Walker {
  private final OpenAPI parser;
  private final Context context = new Context();

  private final List<Visitor> visitors = new LinkedList<>();

  public Walker(final OpenAPI parser) {
    this.parser = parser;
  }

  public Context getContext() {
    return context;
  }

  public OpenAPI getParser() {
    return parser;
  }

  public void walk() {
    final Context context = getContext();
    final OpenAPI parser = getParser();

    if (parser == null) {
      throw new RuntimeException("Could not create parser -- check spec");
    }

    final Components components = parser.getComponents();
    if (components != null) {
      visitors.add(new ComponentSchemasVisitor(context, components.getSchemas()));
      visitors.add(new ComponentResponsesVisitor(context, components.getResponses()));
    }

    if (parser.getPaths() != null) {
      visitors.add(new PathsVisitor(context, parser.getPaths()));
    }

    // now run them all
    visitors.forEach(Visitor::visit);
  }

  public void reset() {
    context.getGeneratedSet().clear();
  }

  public void generate(Writer writer) throws IOException {
    writer.write("scalar JSON\n\n");

    for (Visitor visitor : visitors) {
      visitor.generate(writer);
    }
  }

  public void generatePath(String path, Writer writer) throws IOException {
    // everything should be in Context
    getPathsVisitor().generatePath(path, writer);
  }

  public PathsVisitor getPathsVisitor() {
    Optional<Visitor> first = visitors.stream().filter(v -> v instanceof PathsVisitor).findFirst();
    if (first.isEmpty()) {
      throw new IllegalStateException("No PathsVisitor found -- check OAS schema for valid paths?!");
    }

    return (PathsVisitor) first.get();
  }
}
