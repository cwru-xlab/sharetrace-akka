package org.sharetrace.logging.event;

import org.sharetrace.model.RiskScore;

interface MessageEvent extends LoggableEvent {

  String from();

  String to();

  RiskScore score();

  String id();
}
