package io.sharetrace.util.logging.event;

import io.sharetrace.model.RiskScore;

interface MessageEvent extends LoggableEvent {

  String from();

  String to();

  RiskScore score();

  String id();
}
