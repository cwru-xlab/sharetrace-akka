package org.sharetrace.data.factory;

import org.sharetrace.message.RiskScore;

@FunctionalInterface
public interface ScoreFactory {

  RiskScore getScore(int user);
}
