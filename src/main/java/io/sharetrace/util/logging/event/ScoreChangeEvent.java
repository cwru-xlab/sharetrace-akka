package io.sharetrace.util.logging.event;

import io.sharetrace.model.RiskScore;

interface ScoreChangeEvent extends LoggableEvent {

    RiskScore oldScore();

    RiskScore newScore();

    String oldId();

    String newId();
}
