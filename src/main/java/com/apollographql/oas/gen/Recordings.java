package com.apollographql.oas.gen;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Recordings {

  public static Map<String, String> fromMapInputStream(final InputStream input) {
    Map<String, String> entries = new LinkedHashMap<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;
      while ((line = reader.readLine()) != null) {
//        linesList.add(line);
//        System.out.println("line = " + line);
        final String[] split = line.split("->");
        entries.put(split[0].trim(), split[1].trim());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Convert List to Array
    return entries;
  }

  public final String[] fromFile(final String file) throws IOException {
    final Path filePath = Path.of(file);
    List<String> linesList = Files.readAllLines(filePath);
    return linesList.toArray(new String[0]);
  }

  public final String[] fromResource(final String resourceName) throws IOException {
    InputStream input = Recordings.class.getClassLoader().getResourceAsStream(resourceName);
    assert input != null;

    return fromInputStream(input);
  }

  public static String[] fromInputStream(final InputStream input) {
    List<String> linesList = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;
      while ((line = reader.readLine()) != null) {
        linesList.add(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Convert List to Array
    return linesList.toArray(new String[0]);
  }

}
