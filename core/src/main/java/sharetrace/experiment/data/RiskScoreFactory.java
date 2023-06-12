package sharetrace.experiment.data;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

@FunctionalInterface
public interface RiskScoreFactory<K> {

  RiskScore getScore(K key);

  default RiskScoreFactory<K> cached() {
    return CachedRiskScoreFactory.of(this);
  }

  default RiskScoreFactory<K> noisy(DistributedRandom noise) {
    return NoisyRiskScoreFactory.of(this, noise);
  }
}
