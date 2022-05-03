package org.sharetrace.util.logging;

import org.sharetrace.model.message.RiskScore;

interface ScoreChangeEvent extends LoggableEvent {

  RiskScore oldScore();

  RiskScore newScore();

  String oldUuid();

  String newUuid();
}
