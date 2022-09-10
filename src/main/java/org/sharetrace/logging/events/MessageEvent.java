package org.sharetrace.logging.events;

import org.sharetrace.model.RiskScore;

interface MessageEvent extends LoggableEvent {

  String from();

  String to();

  RiskScore score();

  String id();
}
