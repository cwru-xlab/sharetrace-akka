package sharetrace.actor;

import akka.actor.typed.ActorRef;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.UserParameters;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;

final class UserHelper {

  private final ActorRef<UserMessage> self;
  private final Clock clock;
  private final UserParameters parameters;

  public UserHelper(ActorRef<UserMessage> self, Clock clock, UserParameters parameters) {
    this.self = self;
    this.clock = clock;
    this.parameters = parameters;
  }

  public boolean isAbove(TemporalScore score, TemporalScore threshold) {
    return isAbove(score, threshold.value());
  }

  public boolean isAbove(TemporalScore score, float threshold) {
    return score.value() > threshold + parameters.tolerance();
  }

  public float sendThreshold(TemporalScore score) {
    return score.value() * parameters.sendCoefficient();
  }

  public Instant buffered(Instant timestamp) {
    return timestamp.plus(parameters.timeBuffer());
  }

  public RiskScoreMessage transmitted(RiskScoreMessage message) {
    return message.withSender(self).mapScore(this::transmitted);
  }

  public RiskScoreMessage defaultMessage() {
    return RiskScoreMessage.of(RiskScore.MIN, self);
  }

  public Duration untilExpiry(TemporalScore score) {
    return untilExpiry(score.timestamp(), parameters.scoreExpiry());
  }

  public Duration untilExpiry(Temporal contactTimestamp) {
    return untilExpiry(contactTimestamp, parameters.contactExpiry());
  }

  public boolean isExpired(TemporalScore score) {
    return isExpired(score.timestamp(), parameters.scoreExpiry());
  }

  public boolean isExpired(ContactMessage message) {
    return isExpired(message.timestamp());
  }

  public boolean isExpired(Temporal contactTimestamp) {
    return isExpired(contactTimestamp, parameters.contactExpiry());
  }

  private RiskScore transmitted(RiskScore score) {
    return score.mapValue(value -> value * parameters.transmissionRate());
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
