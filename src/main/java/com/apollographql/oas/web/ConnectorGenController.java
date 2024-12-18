package com.apollographql.oas.web;

import com.apollographql.oas.gen.WebGenerator;
import com.apollographql.oas.gen.nodes.Composed;
import com.apollographql.oas.gen.nodes.GetOp;
import com.apollographql.oas.gen.nodes.Type;
import com.apollographql.oas.gen.nodes.props.Prop;
import com.apollographql.oas.gen.nodes.props.PropScalar;
import com.apollographql.oas.gen.prompt.Input;
import com.apollographql.oas.gen.prompt.Prompt;
import com.apollographql.oas.web.storage.StorageService;
import jakarta.websocket.server.PathParam;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ConnectorGenController {
  private final StorageService storageService;
  private final GeneratorService generatorService;

  @Autowired
  public ConnectorGenController(StorageService storage, GeneratorService generatorService) {
    this.storageService = storage;
    this.generatorService = generatorService;
  }

  @GetMapping("/hello")
  public String sayHello() {
    return "Hello, World!";
  }

  @GetMapping("/hello-json")
  public Map<String, String> sayHelloInJSON() {
    HashMap<String, String> map = new HashMap<>();
    map.put("key", "value");
    map.put("foo", "bar");
    map.put("aa", "bb");
    return map;
  }

  @GetMapping("/list-files")
  public Map<String, String> listUploadedFiles() throws IOException {
    return storageService.loadAll()
      .map(path -> path.getFileName().toString())
      .collect(Collectors.toMap(
        item -> item,          // Key: the item itself
        item -> DigestUtils.md5Hex(item).toUpperCase()  // Value: length of the item
      ));
  }

  @GetMapping("/visit/{md5}/path")
  public Map<String, Object> visitPath(@PathVariable String md5, @PathParam("id") String id) throws IOException {
    final WebGenerator generator = this.generatorService.get(md5);
    System.out.println("ConnectorGenController.visitPath for " + md5 + ", path: " + id);

    final GetOp result = generator.getPathResult(id);

    final Map<String, Object> collected = new LinkedHashMap<>();
    collected.put("parent", result.getName());
    collected.put("parameters", result.getParameters().stream().map(p -> p.getName()).toList());

    final Type resultType = result.getResultType();
    collected.put("result",
      Map.<String, Object>of(
        "id", resultType.id(),
        "path", resultType.path(),
        "name", resultType.getName(),
        "value", (resultType instanceof PropScalar) ? (((PropScalar) resultType).getValue(generator.getContext())) : ""
      )
    );

//      collectTypes(result, collected);
    System.out.println("collected = " + collected);
    return collected;
  }

  @GetMapping("/visit/{md5}/type")
  public Map<String, Object> visitType(@PathVariable String md5, @RequestParam("id") String path, @RequestParam("p") String parent) throws IOException {
    final WebGenerator generator = this.generatorService.get(md5);

    System.out.println("ConnectorGenController.visitType [in]\n" + path);

    final Type found = generator.find(path);

    if (found != null) {
      final Map<String, Object> result = new LinkedHashMap<>();
      result.put("parent", found.getName());

      Collection<? extends Type> values;
      if (found instanceof Composed) {
         values = found.getProps().values();
      }
      else {
        values  = found.getChildren();
      }

      result.put("result", values.stream().map(t -> Map.<String, Object>of(
        "id", t.id(),
        "path", t.path(),
        "name", t.getName(),
        "value", t.forPrompt(generator.getContext())
      )));

      System.out.println("ConnectorGenController.visitType [out] \n" + result);
      return result;
    }
    else {
      return Collections.emptyMap();
    }
  }

  @PostMapping("/visit/{md5}/generate")
  public Map<String, String> generate(@PathVariable String md5, @RequestBody Map<String, String> items) throws IOException {
    final Map<String, String> records = items;
    System.out.println("records = " + records);

    final WebGenerator generator = this.generatorService.get(md5);

    final StringWriter writer = new StringWriter();
    generator.writeSchema(writer, Prompt.create(Prompt.Factory.mapPlayer(records)));

    return Map.of("result", writer.toString());
  }

  @PostMapping("/upload")
  public Map<String, Object> handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
    final Path destination = storageService.store(file);
    final String fileName = file.getOriginalFilename();
    final List<String> paths = generatorService.parse(fileName, destination);

    final Map<String, Object> response = new LinkedHashMap<>();
    response.put("filename", fileName);
    response.put("md5", DigestUtils.md5Hex(fileName).toUpperCase());
    response.put("paths", paths);

    return response;
  }
}