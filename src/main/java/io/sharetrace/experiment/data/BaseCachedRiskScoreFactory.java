package io.sharetrace.experiment.data;

import io.sharetrace.model.RiskScore;
import io.sharetrace.util.Collecting;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseCachedRiskScoreFactory<K> implements RiskScoreFactory<K> {

  @Override
  public RiskScore getScore(K key) {
    return cache().computeIfAbsent(key, scoreFactory()::getScore);
  }

  @Value.Default
  protected Map<K, RiskScore> cache() {
    return Collecting.newHashMap();
  }

  @Value.Parameter
  protected abstract RiskScoreFactory<K> scoreFactory();
}
