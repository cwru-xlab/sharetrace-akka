package sharetrace.experiment;

import com.google.common.collect.Range;
import org.immutables.value.Value;
import sharetrace.util.Checks;

public abstract class AbstractExperiment<K> implements Experiment<K> {

  public abstract int networks();

  public abstract int iterations();

  @Value.Check
  protected void check() {
    Checks.checkRange(iterations(), Range.atLeast(1), "iterations");
    Checks.checkRange(networks(), Range.atLeast(1), "networks");
  }
}
