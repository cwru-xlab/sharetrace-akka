package io.sharetrace.experiment.data.factory;

import io.sharetrace.model.RiskScore;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
    return new Int2ObjectOpenHashMap<>();
  }

  @Value.Parameter
  protected abstract RiskScoreFactory cached();
}
