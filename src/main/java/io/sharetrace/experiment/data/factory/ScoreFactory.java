package io.sharetrace.experiment.data.factory;

import io.sharetrace.model.RiskScore;
import java.util.function.Supplier;
import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface ScoreFactory {

  static ScoreFactory from(Supplier<RiskScore> supplier) {
    return x -> supplier.get();
  }

  RiskScore get(int user);

  default ScoreFactory cached() {
    return CachedScoreFactory.of(this);
  }

  default ScoreFactory noisy(RealDistribution distribution) {
    return NoisyScoreFactory.of(this, distribution);
  }
}
