package org.sharetrace.data.factory;

import org.sharetrace.message.RiskScore;

@FunctionalInterface
public interface RiskScoreFactory {

  RiskScore getRiskScore(int user);
}
