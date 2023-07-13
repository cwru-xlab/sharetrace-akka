// import sharetrace.experiment.BaseRuntimeExperiment;
// import sharetrace.experiment.BaseParametersExperiment;
// import sharetrace.experiment.FileExperiment;
// import sharetrace.experiment.config.FileExperimentConfig;
// import sharetrace.experiment.config.ParamsExperimentConfig;
// import sharetrace.experiment.config.RuntimeExperimentConfig;
// import sharetrace.graph.GraphType;
// import java.nio.file.Path;
// import org.junit.jupiter.api.Assertions;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.EnumSource;
//
// public class ExperimentTests {
//
//  private static final String GRAPH_PATH_TEMPLATE = "src/main/resources/datasets/%s.txt";
//
//  @ParameterizedTest
//  @EnumSource(
//      value = GraphType.class,
//      names = {"INVS13", "INVS15", "LH10", "LYON_SCHOOL", "SFHH", "THIERS11", "THIERS12"})
//  public void testFileExperiment(GraphType graphType) {
//    FileExperimentConfig config =
//        FileExperimentConfig.builder()
//            .graphType(graphType)
//            .path(Path.of(String.format(GRAPH_PATH_TEMPLATE, graphType)))
//            .build();
//    Assertions.assertDoesNotThrow(() -> new FileExperiment().runWithDefaults(config));
//  }
//
//  @ParameterizedTest
//  @EnumSource(
//      value = GraphType.class,
//      names = {"BARABASI_ALBERT", "GNM_RANDOM", "RANDOM_REGULAR", "SCALE_FREE", "WATTS_STROGATZ"})
//  public void testParamsExperiment(GraphType graphType) {
//    ParamsExperimentConfig config =
//        ParamsExperimentConfig.builder()
//            .addTransmissionRate(0.8f)
//            .addSendCoefficient(0.6f)
//            .graphType(graphType)
//            .users(1000)
//            .build();
//    Assertions.assertDoesNotThrow(() -> new
// BaseParametersExperiment().runWithDefaults(config));
//  }
//
//  @ParameterizedTest
//  @EnumSource(
//      value = GraphType.class,
//      names = {"BARABASI_ALBERT", "GNM_RANDOM", "RANDOM_REGULAR", "SCALE_FREE", "WATTS_STROGATZ"})
//  public void testRuntimeExperiment(GraphType graphType) {
//    RuntimeExperimentConfig config =
//        RuntimeExperimentConfig.builder().graphType(graphType).addUsers(1000).build();
//    Assertions.assertDoesNotThrow(() -> new BaseRuntimeExperiment().runWithDefaults(config));
//  }
// }
