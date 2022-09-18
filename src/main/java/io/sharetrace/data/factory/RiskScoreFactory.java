package io.sharetrace.data.factory;

import io.sharetrace.model.RiskScore;
import java.util.function.Supplier;

@FunctionalInterface
public interface RiskScoreFactory {

  static RiskScoreFactory from(Supplier<RiskScore> supplier) {
    return x -> supplier.get();
  }

  RiskScore riskScore(int user);
}
