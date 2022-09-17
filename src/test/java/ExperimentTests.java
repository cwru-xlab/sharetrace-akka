import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sharetrace.experiment.FileExperiment;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.experiment.ParamsExperiment;
import org.sharetrace.experiment.RuntimeExperiment;
import org.sharetrace.experiment.config.FileExperimentConfig;
import org.sharetrace.experiment.config.ParamsExperimentConfig;
import org.sharetrace.experiment.config.RuntimeExperimentConfig;
import org.sharetrace.util.range.FloatRange;
import org.sharetrace.util.range.IntRange;

class ExperimentTests {

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
            .transRates(FloatRange.single(0.8))
            .sendCoeffs(FloatRange.single(0.6))
            .graphType(graphType)
            .numNodes(1000)
            .build();
    Assertions.assertDoesNotThrow(() -> ParamsExperiment.instance().runWithDefaults(config));
  }

  @Test
  public void testRuntimeExperiment() {
    RuntimeExperimentConfig config =
        RuntimeExperimentConfig.builder()
            .graphType(GraphType.GNM_RANDOM)
            .numNodes(IntRange.single(1000))
            .build();
    Assertions.assertDoesNotThrow(() -> RuntimeExperiment.instance().runWithDefaults(config));
  }
}
