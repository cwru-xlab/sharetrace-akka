package sharetrace.model.factory;

import sharetrace.model.RiskScore;
import sharetrace.util.DistributedRandom;

@FunctionalInterface
public interface RiskScoreFactory<K> {

  RiskScore getScore(K key);

  default RiskScoreFactory<K> cached() {
    return new CachedRiskScoreFactory<>(this);
  }

  default RiskScoreFactory<K> withNoise(DistributedRandom noise) {
    return new NoisyRiskScoreFactory<>(this, noise);
  }
}
