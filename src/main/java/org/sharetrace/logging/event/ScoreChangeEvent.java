package org.sharetrace.logging.event;

import org.sharetrace.model.RiskScore;

interface ScoreChangeEvent extends LoggableEvent {

  RiskScore oldScore();

  RiskScore newScore();

  String oldId();

  String newId();
}
