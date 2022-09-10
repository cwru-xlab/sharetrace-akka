package org.sharetrace.data.factory;

import java.util.function.Supplier;
import org.sharetrace.model.RiskScore;

@FunctionalInterface
public interface RiskScoreFactory {

  static RiskScoreFactory from(Supplier<RiskScore> supplier) {
    return x -> supplier.get();
  }

  RiskScore riskScore(int user);
}
