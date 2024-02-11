package sharetrace.model.factory;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import sharetrace.model.RiskScore;

import java.util.Map;

public record CachedRiskScoreFactory(RiskScoreFactory scoreFactory, Map<Integer, RiskScore> cache)
    implements RiskScoreFactory {

  public CachedRiskScoreFactory(RiskScoreFactory scoreFactory) {
    this(scoreFactory, new Int2ReferenceOpenHashMap<>());
  }

  @Override
  public RiskScore getRiskScore(int key) {
    return cache.computeIfAbsent(key, scoreFactory::getRiskScore);
  }
}
