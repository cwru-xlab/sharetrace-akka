package sharetrace.experiment;

import com.google.common.collect.Range;
import org.immutables.value.Value;
import sharetrace.util.Checks;

public abstract class AbstractExperiment<K> implements Experiment<K> {

  public static final int MIN_ITERATIONS = 1;
  public static final int MIN_NETWORKS = 1;

  private static final Range<Integer> ITERATIONS_RANGE = Range.atLeast(MIN_ITERATIONS);
  private static final Range<Integer> NETWORKS_RANGE = Range.atLeast(MIN_NETWORKS);

  public abstract int networks();

  public abstract int iterations();

  @Value.Check
  protected void check() {
    Checks.checkRange(iterations(), ITERATIONS_RANGE, "iterations");
    Checks.checkRange(networks(), NETWORKS_RANGE, "networks");
  }
}
