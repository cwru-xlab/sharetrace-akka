package org.sharetrace.util.logging;

import org.sharetrace.model.message.RiskScore;

interface MessageEvent extends LoggableEvent {

  String from();

  String to();

  RiskScore score();

  String uuid();
}
