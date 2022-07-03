package org.sharetrace.experiment;

import java.util.Objects;
import java.util.Set;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.util.Range;

public class RuntimeExperiment extends SyntheticExperiment {

  private final Range nodes;

  private RuntimeExperiment(Builder builder) {
    super(builder);
    this.nodes = builder.nodes;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected Set<Class<? extends Loggable>> loggable() {
    // Events and path-based graph metrics becomes too expensive for large graphs
    return Set.of(GraphSizeMetrics.class, RuntimeMetric.class, ExperimentSettings.class);
  }

  @Override
  public void run() {
    for (double n : nodes) {
      nNodes = (int) n;
      super.run();
    }
  }

  public static class Builder extends SyntheticExperiment.Builder {
    private Range nodes;

    public Builder nodes(Range nodes) {
      this.nodes = Objects.requireNonNull(nodes);
      return this;
    }

    @Override
    public RuntimeExperiment build() {
      preBuild();
      return new RuntimeExperiment(this);
    }
  }
}
