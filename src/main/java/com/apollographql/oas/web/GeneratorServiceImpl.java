package com.apollographql.oas.web;

import com.apollographql.oas.gen.WebGenerator;
import com.apollographql.oas.gen.prompt.Prompt;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeneratorServiceImpl implements GeneratorService {
  private Map<String, WebGenerator> map = new LinkedHashMap<>();

  @Autowired
  public void init() {
    System.out.println("GeneratorServiceImpl.init");
  }

  public WebGenerator get(final String md5) {
    return map.get(md5);
  }

  @Override
  public List<String> parse(final String fileName, final Path file) throws IOException {
    final WebGenerator generator = WebGenerator.fromFile(file.toAbsolutePath().toString(),
      Prompt.create(Prompt.Factory.yes())
    );
    map.put(DigestUtils.md5Hex(fileName).toUpperCase(), generator);

    return generator.listGetPaths();
  }
}
