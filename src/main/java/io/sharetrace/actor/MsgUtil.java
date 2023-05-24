package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.TemporalScore;
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.ContactMessage;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.model.message.UserMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;

final class MsgUtil {

  private final ActorRef<UserMessage> self;
  private final Clock clock;
  private final UserParameters parameters;

  public MsgUtil(ActorRef<UserMessage> self, Clock clock, UserParameters parameters) {
    this.self = self;
    this.clock = clock;
    this.parameters = parameters;
  }

  public float threshold(TemporalScore score) {
    return score.value() * parameters.sendCoefficient();
  }

  public Instant buffered(Instant contactTime) {
    return contactTime.plus(parameters.timeBuffer());
  }

  public RiskScoreMessage transmitted(RiskScoreMessage message) {
    float value = message.value() * parameters.transmissionRate();
    RiskScore transmitted = message.score().withValue(value);
    return RiskScoreMessage.builder().sender(self).score(transmitted).id(message.id()).build();
  }

  public RiskScoreMessage defaultMessage() {
    return RiskScoreMessage.builder().score(RiskScore.MIN).sender(self).build();
  }

  public Duration scoreTimeToLive(TemporalScore score) {
    return remainingTimeToLive(score.timestamp(), parameters.scoreTimeToLive());
  }

  private Duration remainingTimeToLive(Temporal temporal, Duration timeToLive) {
    return timeToLive.minus(since(temporal));
  }

  private Duration since(Temporal temporal) {
    return Duration.between(temporal, clock.instant());
  }

  public Duration contactTimeToLive(Temporal contactTime) {
    return remainingTimeToLive(contactTime, parameters.contactTimeToLive());
  }

  public boolean isScoreAlive(TemporalScore score) {
    return isAlive(score.timestamp(), parameters.scoreTimeToLive());
  }

  private boolean isAlive(Temporal temporal, Duration timeToLive) {
    return since(temporal).compareTo(timeToLive) < 0;
  }

  public boolean isContactAlive(ContactMessage message) {
    return isContactAlive(message.contactTime());
  }

  public boolean isContactAlive(Temporal contactTime) {
    return isAlive(contactTime, parameters.contactTimeToLive());
  }
}
