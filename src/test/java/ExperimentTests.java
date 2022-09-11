import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sharetrace.experiment.FileExperiment;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.experiment.ParamsExperiment;
import org.sharetrace.experiment.RuntimeExperiment;
import org.sharetrace.util.Ranges;

class ExperimentTests {

  private static final String GRAPH_PATH_TEMPLATE = "src/main/resources/datasets/%s.txt";

  @ParameterizedTest
  @EnumSource(
      value = GraphType.class,
      names = {"INVS13", "INVS15", "LH10", "LYON_SCHOOL", "SFHH", "THIERS11", "THIERS12"})
  public void testFileExperiment(GraphType graphType) {
    Path path = Path.of(String.format(GRAPH_PATH_TEMPLATE, graphType));
    Assertions.assertDoesNotThrow(() -> FileExperiment.create().runWithDefaults(graphType, path));
  }

  @ParameterizedTest
  @EnumSource(
      value = GraphType.class,
      names = {"BARABASI_ALBERT", "GNM_RANDOM", "RANDOM_REGULAR", "SCALE_FREE", "WATTS_STROGATZ"})
  public void testParamsExperiment(GraphType graphType) {
    ParamsExperiment experiment =
        ParamsExperiment.builder()
            .transmissionRates(Ranges.ofFloat(0.8f))
            .sendCoefficients(Ranges.ofFloat(0.6f))
            .build();
    Assertions.assertDoesNotThrow(() -> experiment.runWithDefaults(graphType, 1000));
  }

  @Test
  public void testRuntimeExperiment() {
    RuntimeExperiment experiment = RuntimeExperiment.of(Ranges.ofInt(1000));
    Assertions.assertDoesNotThrow(() -> experiment.runWithDefaults(GraphType.GNM_RANDOM));
  }
}
