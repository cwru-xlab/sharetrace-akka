import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sharetrace.experiment.FileExperiment;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.experiment.ParamsExperiment;
import org.sharetrace.experiment.RuntimeExperiment;
import org.sharetrace.util.range.DoubleRange;
import org.sharetrace.util.range.IntRange;

class ExperimentTests {

  private static final String GRAPH_PATH_TEMPLATE = "src/main/resources/datasets/%s.txt";

  @ParameterizedTest
  @EnumSource(
      value = GraphType.class,
      names = {"INVS13", "INVS15", "LH10", "LYON_SCHOOL", "SFHH", "THIERS11", "THIERS12"})
  public void testFileExperiment(GraphType graphType) {
    Path path = Path.of(String.format(GRAPH_PATH_TEMPLATE, graphType));
    FileExperiment.Inputs inputs = FileExperiment.Inputs.of(graphType, path);
    Assertions.assertDoesNotThrow(() -> FileExperiment.create().runWithDefaults(inputs));
  }

  @ParameterizedTest
  @EnumSource(
      value = GraphType.class,
      names = {"BARABASI_ALBERT", "GNM_RANDOM", "RANDOM_REGULAR", "SCALE_FREE", "WATTS_STROGATZ"})
  public void testParamsExperiment(GraphType graphType) {
    ParamsExperiment experiment =
        ParamsExperiment.builder()
            .transRates(DoubleRange.single(0.8))
            .sendCoeffs(DoubleRange.single(0.6))
            .build();
    ParamsExperiment.Inputs inputs = ParamsExperiment.Inputs.of(graphType, 1000);
    Assertions.assertDoesNotThrow(() -> experiment.runWithDefaults(inputs));
  }

  @Test
  public void testRuntimeExperiment() {
    RuntimeExperiment experiment = RuntimeExperiment.of(IntRange.single(1000));
    RuntimeExperiment.Inputs inputs = RuntimeExperiment.Inputs.of(GraphType.GNM_RANDOM);
    Assertions.assertDoesNotThrow(() -> experiment.runWithDefaults(inputs));
  }
}
