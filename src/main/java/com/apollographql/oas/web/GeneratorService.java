package com.apollographql.oas.web;

import com.apollographql.oas.gen.WebGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface GeneratorService {
  void init();

  public WebGenerator get(final String md5);

  public List<String> parse(final String fileName, final Path file) throws IOException;
}
