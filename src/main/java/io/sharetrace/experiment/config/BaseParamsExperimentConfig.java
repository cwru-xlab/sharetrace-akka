package io.sharetrace.experiment.config;

import io.sharetrace.experiment.GraphType;
import io.sharetrace.util.Checks;
import io.sharetrace.util.range.FloatRange;
import io.sharetrace.util.range.Range;
import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseParamsExperimentConfig {

  public abstract Optional<GraphType> graphType();

  public abstract OptionalInt numNodes();

  @Value.Default
  public Range<Float> transRates() {
    return FloatRange.of(0.1, 1.0, 0.1);
  }

  @Value.Default
  public Range<Float> sendCoeffs() {
    return FloatRange.of(0.1, 1.1, 0.1);
  }

  @Value.Default
  public int numIterations() {
    return 1;
  }

  @Value.Check
  protected void check() {
    Checks.isAtLeast(numIterations(), 1, "numIterations");
  }
}
