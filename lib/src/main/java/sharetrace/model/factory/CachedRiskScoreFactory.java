package sharetrace.model.factory;

import java.util.HashMap;
import java.util.Map;
import sharetrace.model.RiskScore;

public record CachedRiskScoreFactory<K>(RiskScoreFactory<K> scoreFactory, Map<K, RiskScore> cache)
    implements RiskScoreFactory<K> {

  public CachedRiskScoreFactory(RiskScoreFactory<K> scoreFactory) {
    this(scoreFactory, new HashMap<>());
  }

  @Override
  public RiskScore getScore(K key) {
    return cache.computeIfAbsent(key, scoreFactory::getScore);
  }
}
