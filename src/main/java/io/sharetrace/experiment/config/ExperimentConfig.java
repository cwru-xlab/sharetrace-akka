package io.sharetrace.experiment.config;

import com.google.common.collect.Range;
import io.sharetrace.graph.GraphType;
import io.sharetrace.util.Checks;
import java.util.Optional;
import org.immutables.value.Value;

abstract class ExperimentConfig {

  public static final int MIN_ITERATIONS = 1;

  private static final Range<Integer> ITERATIONS_RANGE = Range.atLeast(MIN_ITERATIONS);

  public abstract Optional<GraphType> graphType();

  @Value.Check
  protected void check() {
    Checks.inRange(numIterations(), ITERATIONS_RANGE, "numIterations");
  }

  @Value.Default
  public int numIterations() {
    return MIN_ITERATIONS;
  }
}
