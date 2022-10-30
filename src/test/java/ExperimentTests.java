import io.sharetrace.experiment.FileExperiment;
import io.sharetrace.experiment.ParamsExperiment;
import io.sharetrace.experiment.RuntimeExperiment;
import io.sharetrace.experiment.config.FileExperimentConfig;
import io.sharetrace.experiment.config.ParamsExperimentConfig;
import io.sharetrace.experiment.config.RuntimeExperimentConfig;
import io.sharetrace.graph.GraphType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ExperimentTests {

  private static final String GRAPH_PATH_TEMPLATE = "src/main/resources/datasets/%s.txt";

  @ParameterizedTest
  @EnumSource(
      value = GraphType.class,
      names = {"INVS13", "INVS15", "LH10", "LYON_SCHOOL", "SFHH", "THIERS11", "THIERS12"})
  public void testFileExperiment(GraphType graphType) {
    FileExperimentConfig config =
        FileExperimentConfig.builder()
            .graphType(graphType)
            .path(Path.of(String.format(GRAPH_PATH_TEMPLATE, graphType)))
            .build();
    Assertions.assertDoesNotThrow(() -> FileExperiment.instance().runWithDefaults(config));
  }

  @ParameterizedTest
  @EnumSource(
      value = GraphType.class,
      names = {"BARABASI_ALBERT", "GNM_RANDOM", "RANDOM_REGULAR", "SCALE_FREE", "WATTS_STROGATZ"})
  public void testParamsExperiment(GraphType graphType) {
    ParamsExperimentConfig config =
        ParamsExperimentConfig.builder()
            .transRates(List.of(0.8f))
            .sendCoeffs(List.of(0.6f))
            .graphType(graphType)
            .numNodes(1000)
            .build();
    Assertions.assertDoesNotThrow(() -> ParamsExperiment.instance().runWithDefaults(config));
  }

  @ParameterizedTest
  @EnumSource(
      value = GraphType.class,
      names = {"BARABASI_ALBERT", "GNM_RANDOM", "RANDOM_REGULAR", "SCALE_FREE", "WATTS_STROGATZ"})
  public void testRuntimeExperiment(GraphType graphType) {
    RuntimeExperimentConfig config =
        RuntimeExperimentConfig.builder().graphType(graphType).numNodes(List.of(1000)).build();
    Assertions.assertDoesNotThrow(() -> RuntimeExperiment.instance().runWithDefaults(config));
  }
}
