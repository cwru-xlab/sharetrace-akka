package sharetrace.experiment.data;

import com.google.common.collect.Maps;
import java.util.Map;
import org.immutables.value.Value;
import sharetrace.model.RiskScore;

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
    return Maps.newHashMap();
  }
}
