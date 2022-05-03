package org.sharetrace.logging;

import org.sharetrace.message.RiskScore;

interface ScoreChangeEvent extends LoggableEvent {

  RiskScore oldScore();

  RiskScore newScore();

  String oldUuid();

  String newUuid();
}
