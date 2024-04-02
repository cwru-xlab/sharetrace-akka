package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sharetrace.model.RiskScore;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface RiskScoreFactory {

  @JsonProperty
  String id();

  RiskScore getRiskScore(int key);
}
