package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.Map;
import sharetrace.model.RiskScore;

public record CachedRiskScoreFactory(
    @JsonValue RiskScoreFactory factory, Map<Integer, RiskScore> cache)
    implements RiskScoreFactory {

  public CachedRiskScoreFactory(RiskScoreFactory factory) {
    this(factory, new Int2ReferenceOpenHashMap<>());
  }

  @Override
  public String id() {
    return factory.id();
  }

  @Override
  public String type() {
    return factory.type();
  }

  @Override
  public RiskScore getRiskScore(int key) {
    return cache.computeIfAbsent(key, factory::getRiskScore);
  }
}
