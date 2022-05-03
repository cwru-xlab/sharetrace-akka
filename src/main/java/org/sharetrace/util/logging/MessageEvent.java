package org.sharetrace.util.logging;

import org.sharetrace.message.RiskScore;

interface MessageEvent extends LoggableEvent {

  String from();

  String to();

  RiskScore score();

  String uuid();
}
