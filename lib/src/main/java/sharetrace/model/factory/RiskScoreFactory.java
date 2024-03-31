package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sharetrace.model.RiskScore;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@FunctionalInterface
public interface RiskScoreFactory {

  RiskScore getRiskScore(int key);
}
