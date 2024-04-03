package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonProperty;
import sharetrace.model.RiskScore;

public interface RiskScoreFactory {

  @JsonProperty
  String id();

  @JsonProperty
  String type();

  RiskScore getRiskScore(int key);
}
