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

final class UserHelper {

  private final ActorRef<UserMessage> self;
  private final Clock clock;
  private final UserParameters parameters;

  public UserHelper(ActorRef<UserMessage> self, Clock clock, UserParameters parameters) {
    this.self = self;
    this.clock = clock;
    this.parameters = parameters;
  }

  public float threshold(TemporalScore score) {
    return score.value() * parameters.sendCoefficient();
  }

  public Instant buffered(Instant timestamp) {
    return timestamp.plus(parameters.timeBuffer());
  }

  public RiskScoreMessage transmitted(RiskScoreMessage message) {
    return message.withSender(self).mapScore(this::transmitted);
  }

  private RiskScore transmitted(RiskScore score) {
    return score.mapValue(value -> value * parameters.transmissionRate());
  }

  public RiskScoreMessage defaultMessage() {
    return RiskScoreMessage.of(RiskScore.MIN, self);
  }

  public Duration untilExpiry(TemporalScore score) {
    return untilExpiry(score.timestamp(), parameters.scoreExpiry());
  }

  public Duration untilExpiry(Temporal contactTime) {
    return untilExpiry(contactTime, parameters.contactExpiry());
  }

  public boolean isExpired(TemporalScore score) {
    return isExpired(score.timestamp(), parameters.scoreExpiry());
  }

  public boolean isExpired(ContactMessage message) {
    return isExpired(message.timestamp());
  }

  public boolean isExpired(Temporal contactTime) {
    return isExpired(contactTime, parameters.contactExpiry());
  }

  private Duration untilExpiry(Temporal temporal, Duration expiry) {
    return expiry.minus(since(temporal));
  }

  private Duration since(Temporal temporal) {
    return Duration.between(temporal, clock.instant());
  }

  private boolean isExpired(Temporal temporal, Duration expiry) {
    return since(temporal).compareTo(expiry) < 0;
  }
}
