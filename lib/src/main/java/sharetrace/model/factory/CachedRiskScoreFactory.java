package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.Map;
import sharetrace.model.RiskScore;

@JsonTypeName("Cached")
public record CachedRiskScoreFactory(
    RiskScoreFactory factory, @JsonIgnore Map<Integer, RiskScore> cache)
    implements RiskScoreFactory {

  public CachedRiskScoreFactory(RiskScoreFactory factory) {
    this(factory, new Int2ReferenceOpenHashMap<>());
  }

  @Override
  public RiskScore getRiskScore(int key) {
    return cache.computeIfAbsent(key, factory::getRiskScore);
  }
}
