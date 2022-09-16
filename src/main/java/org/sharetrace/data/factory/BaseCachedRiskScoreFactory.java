package org.sharetrace.data.factory;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import org.immutables.value.Value;
import org.sharetrace.model.RiskScore;

@Value.Immutable
abstract class BaseCachedRiskScoreFactory implements RiskScoreFactory {

  @Override
  public RiskScore riskScore(int user) {
    return cache().computeIfAbsent(user, scoreFactory()::riskScore);
  }

  @Value.Parameter
  protected abstract RiskScoreFactory scoreFactory();

  @Value.Lazy
  protected Map<Integer, RiskScore> cache() {
    return new Int2ObjectOpenHashMap<>();
  }
}
