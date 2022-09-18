package io.sharetrace.logging.event;

import io.sharetrace.model.RiskScore;

interface MessageEvent extends LoggableEvent {

  String from();

  String to();

  RiskScore score();

  String id();
}
