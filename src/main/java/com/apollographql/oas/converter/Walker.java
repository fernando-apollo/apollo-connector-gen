package com.apollographql.oas.converter;

import com.apollographql.oas.converter.context.Context;
import com.apollographql.oas.converter.visitor.ComponentResponsesVisitor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import com.apollographql.oas.converter.visitor.ComponentSchemasVisitor;
import com.apollographql.oas.converter.visitor.PathsVisitor;
import com.apollographql.oas.converter.visitor.Visitor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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

  public void generate(Writer writer) throws IOException {
    System.out.println("-------------------------------- GQL ------------------------------------");
    writer.write("scalar JSON\n\n");

//    if (!context.getOperations().isEmpty()) {
//      writer.write("type Query {\n");
//      for (Map.Entry<String, CType> entry : context.getOperations().entrySet()) {
//        entry.getValue().generate(context, writer);
//      }
//      writer.write("}\n\n");
//    }
//
//    for (Map.Entry<String, CType> entry : context.getTypes().entrySet()) {
//      entry.getValue().generate(context, writer);
//    }
    for (Visitor visitor : visitors) {
      visitor.generate(writer);
    }
  }

  public void generatePath(String path, Writer writer) throws IOException {
    // everything should be in Context
    Optional<Visitor> first = visitors.stream().filter(v -> v instanceof PathsVisitor).findFirst();

    if (first.isEmpty()) {
      throw new IllegalStateException("No PathsVisitor found -- check OAS schema for valid paths?!");
    }

    final PathsVisitor paths = (PathsVisitor) first.get();
    paths.generatePath(path, writer);
  }
}
