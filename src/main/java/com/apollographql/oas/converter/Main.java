package com.apollographql.oas.converter;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Main {
  public static void main(String[] args) throws IOException {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    final String baseURL = "/Users/fernando/Documents/Opportunities/Vodafone/tmf-apis";
//    final String source = String.format("%s/sample-oas/petstore.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF678-CustomerBill-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF632-Party_Management-v5.0.0.oas.yaml", baseURL);
    final String source = String.format("%s/tmf-specs/TMF666-Account_Management-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF620-ProductCatalog-v4.1.0.swagger.json", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF637-ProductInventory-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF680-Recommendation-v4.0.0.swagger.json", baseURL);
//    final String source = String.format("%s/tmf-specs/t-boolean-array.yml", baseURL);

    if (!new File(source).exists()) {
      throw new FileNotFoundException("Source not found: " + source);
    }

    final OpenAPI parser = new OpenAPIV3Parser().read(source, null, options);

    final Walker walker = new Walker(parser);
    walker.walk();

//    if (true) {
//      StringWriter writer = new StringWriter();
//      walker.generatePath("/individual/{id}", writer);
//      writer.flush();
//      System.out.println("Main.main -> \n" + writer);
//      return;
//    }

    Scanner scanner = new Scanner(System.in);
    int choice = -1;
    do {
      final List<String> items = new ArrayList<>(walker.getPathsVisitor().getPaths());
      items.add(0, "---- full schema");
      items.add(0, "---- reset context");
      items.add(0, "---- exit");

      System.out.println("Choose an item from the list:");
      for (int i = 0; i < items.size(); i++) {
        System.out.println((i + 1) + ". " + items.get(i));
      }

      System.out.print("Enter the number of your choice: ");
      choice = scanner.nextInt();

      if (choice > 3 && choice <= items.size()) {
        final String path = items.get(choice - 1);
        System.out.println("> Chosen path: " + path);

//        final OutputStreamWriter writer = new OutputStreamWriter(System.out);
        final StringWriter writer = new StringWriter();
        walker.generatePath(path, writer);
        writer.flush();
        System.out.println(writer);

      } else if (choice == 2) {
        System.out.println("(!) Walker has been reset");
        walker.reset();
      }
      else if (choice == 3) {
        System.out.println("> Generating full schema");
        walker.reset();

//        final OutputStreamWriter writer = new OutputStreamWriter(System.out);
        final StringWriter writer = new StringWriter();
        walker.generate(writer);
        writer.flush();
        System.out.println(writer);
      }

    } while (choice != 1);

    scanner.close();
  }
}