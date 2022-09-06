package org.sharetrace.logging.events;

import org.sharetrace.message.RiskScore;

interface ScoreChangeEvent extends LoggableEvent {

  RiskScore oldScore();

  RiskScore newScore();

  String oldId();

  String newId();
}
