package sharetrace.model.factory;

import java.util.HashMap;
import java.util.Map;
import sharetrace.model.RiskScore;

public record CachedRiskScoreFactory(RiskScoreFactory scoreFactory, Map<Object, RiskScore> cache)
    implements RiskScoreFactory {

  public CachedRiskScoreFactory(RiskScoreFactory scoreFactory) {
    this(scoreFactory, new HashMap<>());
  }

  @Override
  public RiskScore getRiskScore(Object key) {
    return cache.computeIfAbsent(key, scoreFactory::getRiskScore);
  }
}
