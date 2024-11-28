package com.apollographql.oas.converter;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.IOException;
import java.io.StringWriter;


public class Main {
  public static void main(String[] args) {
//    OpenAPI parser = new OpenAPIV3Parser().read("file:///Users/fernando/Documents/Opportunities/Vodafone/tmf-apis/tmf-specs/TMF678-CustomerBill-v5.0.0.oas.yaml");
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
//      options.setFlatten(true);
//    options.setResolveFully(true);
    options.setResolveCombinators(false); // default is true

    final String baseURL = "file:///Users/fernando/Documents/Opportunities/Vodafone/tmf-apis";

//    final String source = String.format("%s/sample-oas/petstore.yml", baseURL);
    final String source = String.format("%s/tmf-specs/TMF678-CustomerBill-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF632-Party_Management-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF666-Account_Management-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF620-ProductCatalog-v4.1.0.swagger.json", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF637-ProductInventory-v5.0.0.oas.yaml", baseURL);
//    final String source = String.format("%s/tmf-specs/TMF680-Recommendation-v4.0.0.swagger.json", baseURL);
//    final String source = String.format("%s/tmf-specs/t-boolean-array.yml", baseURL);

    final OpenAPI parser = new OpenAPIV3Parser().read(source, null, options);

    final Walker walker = new Walker(parser);
    walker.walk();
    try {
      final StringWriter writer = new StringWriter();
      walker.generate(writer);

      System.out.println(writer);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}