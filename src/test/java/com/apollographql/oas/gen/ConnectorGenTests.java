package com.apollographql.oas.gen;

import com.apollographql.oas.gen.nodes.*;
import com.apollographql.oas.gen.nodes.params.Param;
import com.apollographql.oas.gen.nodes.props.Prop;
import com.apollographql.oas.gen.nodes.props.PropArray;
import com.apollographql.oas.gen.nodes.props.PropScalar;
import com.apollographql.oas.gen.prompt.Prompt;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectorGenTests {
  private StringWriter writer;

  @BeforeEach
  void setUp() throws IOException {
    InputStream configFile = Main.class.getClassLoader().getResourceAsStream("logging.properties");

    if (configFile == null) {
      throw new IllegalArgumentException("logging.properties file not found in classpath");
    }

    // Load the configuration
    LogManager.getLogManager().readConfiguration(configFile);

    System.out.println("ParserTest.setUp creating writer...");
    this.writer = new StringWriter();
  }

  //  @AfterEach
  ImmutablePair<Integer, String> checkCompose() throws IOException, InterruptedException {
    final String schema = getWriter().toString();
    return Rover.compose(schema);
  }

  private StringWriter getWriter() {
    return writer;
  }

  @Test
  void test_001_testMinimalPetstore() throws IOException, InterruptedException {
    final Prompt prompt = loadRecording("test_001_testMinimalPetstore.txt");

    final OpenAPI parser = createParser(loadSpec("petstore.yaml"));
    assertNotNull(parser);

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();

    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);
    assertEquals(1, collected.size(), "Should have collected 5 paths");

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    final Set<Type> childrenSet = new LinkedHashSet<>();
    findAllChildren(type, childrenSet);

    assertFalse(childrenSet.isEmpty(), "Should have found many children");
    assertEquals(10, childrenSet.size());

    final Map<String, Type> types = generator.getContext().getTypes();
    assertEquals(1, types.size());
    assertTrue(types.containsKey("#/components/schemas/Pet"), "Should contain definition for Pet");
    assertInstanceOf(Obj.class, types.get("#/components/schemas/Pet"));

    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_002_testFullPetStoreSchema() throws URISyntaxException, IOException, InterruptedException {
    final Prompt prompt = loadMapRecording("test_001_FullPetstoreSchema.txt");

    final OpenAPI parser = createParser(loadSpec("petstore.yaml"));
    assertNotNull(parser);

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    assertNotNull(generator.getCollected());
    assertEquals(5, generator.getCollected().size(), "Should have collected 5 paths: " + generator.getCollected());

    final Map<String, Type> types = generator.getContext().getTypes();
    assertEquals(5, types.size());
    assertTrue(types.containsKey("#/components/schemas/Pet"), "Should contain definition for Pet");
    assertTrue(types.containsKey("#/components/schemas/Category"), "Should contain definition for Category");
    assertTrue(types.containsKey("#/components/schemas/Tag"), "Should contain definition for Tag");

    assertInstanceOf(Obj.class, types.get("#/components/schemas/Pet"));
    assertInstanceOf(Obj.class, types.get("#/components/schemas/Category"));
    assertInstanceOf(Obj.class, types.get("#/components/schemas/Tag"));

    final Prop tags = types.get("#/components/schemas/Pet").getProps().get("tags");
    assertInstanceOf(PropArray.class, tags);

    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_003_testConsumerJourney() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-consumer-info_v1.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_001_ConsumerJourney.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    final GetOp op = (GetOp) type;
    final List<Param> parameters = op.getParameters();
    assertEquals(3, parameters.size());

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_004_testConsumerJourneyScalarsOnly() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-consumer-info_v1.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_001_ConsumerJourneyScalarsOnly.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);
    assertEquals(type.getChildren().size(), 1);

    Type res = type.getChildren().get(0); // res
    assertInstanceOf(Response.class, res);
    assertEquals(res.getChildren().size(), 1);

    Type ref = res.getChildren().get(0); // ref to Consumer
    assertInstanceOf(Ref.class, ref);
    assertEquals(ref.getChildren().size(), 1);

    Type obj = ref.getChildren().get(0); // obj to Consumer
    assertInstanceOf(Obj.class, obj);
    assertEquals(obj.getProps().size(), 7);

    for (final Prop prop : obj.getProps().values()) {
      assertInstanceOf(PropScalar.class, prop);
    }

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_003_testFindType() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-consumer-info_v1.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_003_testFindType.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    final String p = "get:/consumer/{id}>res:r>ref:#/c/s/Consumer>obj:#/c/s/Consumer>prop:array:#contactMedium>prop:ref:#items";
    final Type sought = Type.findTypeIn(p, collected);

    assertNotNull(sought);
    assertEquals(sought.path(), p);

    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_004_testAccountSegment() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-consumer-info_v1.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_004_testAccountSegment.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();

    assertNotNull(collected);
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_005_testHomepageProductSelector() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-homepage-product-selector_v3.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_001_testHomepageProductSelector.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_006_testHomepageProductSelectorInlineArray() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-homepage-product-selector_v3.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_002_testHomepageProductSelectorInlineArray.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    // TODO: add assertions
//    final GetOp op = (GetOp) type;
//    final List<Param> parameters = op.getParameters();
//    assertEquals(3, parameters.size());

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_007_testHomepageProductSelectorRecursion() throws IOException, InterruptedException {
    // TODO: pending
    final OpenAPI parser = createParser(loadSpec("js-mva-homepage-product-selector_v3.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_002_testHomepageProductSelectorInlineArray.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_008_testHomepageProductSelectorAnonymousObject() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-homepage-product-selector_v3.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_003_testHomepageProductSelectorAnonymousObject.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_009_Customer360_ScalarsOnly() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("TMF717_Customer360-v5.0.0.oas.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_004_Customer360_ScalarsOnly.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);
    assertEquals(collected.size(), 1);

    final Type resultType = ((GetOp) collected.toArray()[0]).getResultType();
    assertInstanceOf(Response.class, resultType);
    final Response response = (Response) resultType;

    final Type responseType = response.getResponseType();
    assertInstanceOf(Ref.class, responseType);

    final Ref ref = (Ref) responseType;
    assertNotNull(ref);

    final Composed composed = (Composed) ((List) ref.getChildren()).get(0);
    assertEquals(composed.getName(), "#/components/schemas/Customer360");
    assertNotNull(composed);

    final Map<String, Prop> props = composed.getProps();
    assertEquals(props.size(), 1);

    final Prop id = props.get("id");
    assertNotNull(id);
    assertInstanceOf(PropScalar.class, id);

    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_010_TMF633_IntentOrValue_to_Union() throws IOException, InterruptedException {
    final Prompt prompt = loadRecording("TMF633_IntentOrValue_to_Union.txt");

    final OpenAPI parser = createParser(loadSpec("TMF637-001-UnionTest.yaml"));
    assertNotNull(parser);

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_011_TMF637_001_ComposedTest() throws IOException, InterruptedException {
    final Prompt prompt = loadRecording("test_TMF637_001_ComposedTest.txt");

    final OpenAPI parser = createParser(loadSpec("TMF637-001-ComposedTest.yaml"));
    assertNotNull(parser);

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();

    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    final Map<String, Type> types = generator.getContext().getTypes();
    assertTrue(types.containsKey("#/components/schemas/Product"));
//    assertTrue(types.containsKey("#/components/schemas/BillingAccountRef"));
    assertTrue(types.containsKey("#/components/schemas/Product"));

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_012_TMF633_Attachment() throws IOException, InterruptedException {
    final Prompt prompt = loadMapRecording("test_TMF633_Attachment.txt");

    final OpenAPI parser = createParser(loadSpec("TMF637-ProductInventory-v5.0.0.oas.yaml"));
    assertNotNull(parser);

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_013_testTMF637_TestSimpleRecursion() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("TMF637-002-SimpleRecursionTest.yaml"));
    assertNotNull(parser);

    final Prompt prompt = Prompt.create(Prompt.Factory.yes());

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();

    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

    System.out.println("ConnectorGenTests.test_004_testTMF637_TestSimpleRecursion ----------- schema -------------- \n");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(1, result.getLeft());
    assertTrue(result.getRight().contains("CIRCULAR_REFERENCE:"));
  }

  @Test
  void test_014_testTMF637_TestRecursion() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("TMF637-002-RecursionTest.yaml"));
    assertNotNull(parser);

    final Prompt prompt = Prompt.create(Prompt.Factory.yes());

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();

    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

    System.out.println("ConnectorGenTests.test_004_testTMF637_TestRecursion ----------- schema -------------- \n");
    generator.writeSchema(getWriter());

    // TODO: need to check the output of the compose command to verify there's a circular error
    final Pair<Integer, String> result = checkCompose();
    assertEquals(1, result.getLeft());
    assertTrue(result.getRight().contains("CIRCULAR_REFERENCE:"));
  }

  @Test
  void test_015_testTMF637_ProductStatusEnum() throws IOException, InterruptedException {
    final Prompt prompt = loadMapRecording("test_015_testTMF637_ProductStatusEnum.txt");

    final OpenAPI parser = createParser(loadSpec("TMF637-ProductInventory-v5.0.0.oas.yaml"));
    assertNotNull(parser);

//    final Prompt prompt = Prompt.create(Prompt.Factory.yes());

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

//    final RefCounter counter = new RefCounter();
//    counter.addAll(collected);
//    System.out.println("ConnectorGenTests.test_003_testTMF637_Full -- \n");
//    final Map<String, Integer> values = counter.getCount();
//    printRefs(values);

    System.out.println("ConnectorGenTests.test_003_testTMF637_Full ----------- schema -------------- \n");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_016_testMostPopularProductScalarsOnly() throws IOException {
    final OpenAPI parser = createParser(loadSpec("most-popular-product.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_001_testMostPopularProductScalarsOnly.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

    generator.writeSchema(getWriter());
  }

  @Test
  void test_017_testMostPopularProduct() throws IOException {
    final OpenAPI parser = createParser(loadSpec("most-popular-product.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_001_testMostPopularProduct.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

    generator.writeSchema(getWriter());
  }

  @Test
  void test_018_testTMF637_Full() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("TMF637-ProductInventory-v5.0.0.oas.yaml"));
    assertNotNull(parser);

    final Prompt prompt = Prompt.create(Prompt.Factory.yes());

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");

//    final RefCounter counter = new RefCounter();
//    counter.addAll(collected);
//    System.out.println("ConnectorGenTests.test_003_testTMF637_Full -- \n");
//    final Map<String, Integer> values = counter.getCount();
//    printRefs(values);

    System.out.println("ConnectorGenTests.test_003_testTMF637_Full ----------- schema -------------- \n");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(1, result.getLeft());
    assertTrue(result.getRight().contains("CIRCULAR_REFERENCE:"));
  }

  @Test
  void test_019_testUnionInParam() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-consumer-info_v1.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_001_ConsumerJourney.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    final GetOp op = (GetOp) type;
    final List<Param> parameters = op.getParameters();
    assertEquals(3, parameters.size());

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_020_testDuplicateRefPath() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-homepage-product-selector_v3.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_020_testDuplicateRefPath.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    final GetOp op = (GetOp) type;
    final List<Param> parameters = op.getParameters();
    assertEquals(5, parameters.size());

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  @Test
  void test_021_testInlineItemsArray() throws IOException, InterruptedException {
    final OpenAPI parser = createParser(loadSpec("js-mva-homepage-product-selector_v3.yaml"));
    assertNotNull(parser);

    final Prompt prompt = loadMapRecording("test_021_testInlineItemsArray.txt");

    final ConnectorGen generator = new ConnectorGen(parser, prompt);
    generator.visit();
    final Set<Type> collected = generator.getCollected();
    assertNotNull(collected);

    assertTrue(collected.stream().findFirst().isPresent(), "First collected should be present");
    final Type type = collected.stream().findFirst().get();

    assertInstanceOf(GetOp.class, type);

    final GetOp op = (GetOp) type;
    final List<Param> parameters = op.getParameters();
    assertEquals(2, parameters.size());

    System.out.println(" ----------- schema -------------- ");
    generator.writeSchema(getWriter());
    final Pair<Integer, String> result = checkCompose();
    assertEquals(0, result.getLeft());
  }

  private static OpenAPI createParser(String source) {
    final ParseOptions options = new ParseOptions();
    options.setResolve(true); // implicit
    options.setResolveCombinators(false); // default is true

    return new OpenAPIV3Parser().read(source, null, options);
  }

  void findAllChildren(Type type, Set<Type> childrenSet) {
    childrenSet.add(type);
    for (Type child : type.getChildren()) {
      findAllChildren(child, childrenSet);
    }
  }

  private static String loadSpec(final String resource) {
    URL input = ConnectorGenTests.class.getClassLoader()
      .getResource(resource);
    assertNotNull(input);

    return input.getPath();
  }

  private static Prompt loadRecording(final String resource) {
    InputStream input = ConnectorGenTests.class.getClassLoader()
      .getResourceAsStream(resource);
    assertNotNull(input);

    final String[] recording = Recordings.fromInputStream(input);
    assertNotNull(recording);
    assertTrue(recording.length > 0);

    return Prompt.create(Prompt.Factory.player(recording));
  }

  private Prompt loadMapRecording(final String resource) {
    InputStream input = ConnectorGenTests.class.getClassLoader()
      .getResourceAsStream(resource);
    assertNotNull(input);

    final Map<String, String> recording = Recordings.fromMapInputStream(input);
    assertNotNull(recording);
    assertTrue(recording.size() > 0);

    return Prompt.create(Prompt.Factory.mapPlayer(recording));
  }

  static class Rover {
    public static void main(String[] args) throws IOException, InterruptedException {
      compose("nothing");
    }

    public static ImmutablePair<Integer, String> compose(final String schema) throws IOException, InterruptedException {
      final String basePath = "/Users/fernando/Documents/Opportunities/Vodafone/tmf-apis/supergraph";
      final Path path = Paths.get(basePath + "/test-spec.graphql");
      Files.write(path, schema.getBytes());

      final String command = String.format("/Users/fernando/.rover/bin/rover supergraph compose --config %s/supergraph.yaml", basePath); // Replace with your desired command
      System.out.println("command = " + command);

      // Run the command
      final ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
      final Process process = processBuilder.start();

      // Read the command output
      final BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      // also collect the stream
      final StringWriter writer = new StringWriter();

      String e;
      while ((e = error.readLine()) != null) {
        System.err.println(e);
        writer.write(e);
      }

      // Wait for the process to finish and get the exit code
      final int errorCode = process.waitFor();

      return new ImmutablePair<Integer, String>(errorCode, writer.toString());
//      return errorCode;
    }
  }
}
