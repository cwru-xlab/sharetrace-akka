package sharetrace.experiment.data;

import java.util.Map;
import org.immutables.value.Value;
import sharetrace.model.RiskScore;
import sharetrace.util.Collecting;

@Value.Immutable
abstract class BaseCachedRiskScoreFactory<K> implements RiskScoreFactory<K> {

  @Override
  public RiskScore getScore(K key) {
    return cache().computeIfAbsent(key, scoreFactory()::getScore);
  }

  @Value.Parameter
  protected abstract RiskScoreFactory<K> scoreFactory();

  @Value.Default
  protected Map<K, RiskScore> cache() {
    return Collecting.newHashMap();
  }
}
