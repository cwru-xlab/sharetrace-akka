package io.sharetrace.experiment.data.factory;

import io.sharetrace.model.RiskScore;
import io.sharetrace.util.Collecting;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseCachedRiskScoreFactory implements RiskScoreFactory {

  @Override
  public RiskScore get(int user) {
    return cache().computeIfAbsent(user, cached()::get);
  }

  @Value.Lazy
  protected Map<Integer, RiskScore> cache() {
    return Collecting.newIntKeyedHashMap();
  }

  @Value.Parameter
  protected abstract RiskScoreFactory cached();
}
