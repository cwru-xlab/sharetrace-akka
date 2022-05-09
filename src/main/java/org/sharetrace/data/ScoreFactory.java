package org.sharetrace.data;

import org.sharetrace.message.RiskScore;

@FunctionalInterface
public interface ScoreFactory {

  RiskScore getScore(int node);
}
