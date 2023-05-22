package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.TemporalProbability;
import io.sharetrace.model.UserParams;
import io.sharetrace.model.message.ContactMsg;
import io.sharetrace.model.message.RiskScoreMsg;
import io.sharetrace.model.message.UserMsg;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;

final class MsgUtil {

  private final ActorRef<UserMsg> self;
  private final Clock clock;
  private final UserParams params;

  public MsgUtil(ActorRef<UserMsg> self, Clock clock, UserParams params) {
    this.self = self;
    this.clock = clock;
    this.params = params;
  }

  public boolean isNotAfter(TemporalProbability msg, Instant time) {
    return !msg.time().isAfter(time);
  }

  public float computeThreshold(TemporalProbability msg) {
    return msg.value() * params.sendCoeff();
  }

  public Instant buffered(Instant instant) {
    return instant.plus(params.timeBuffer());
  }

  public RiskScoreMsg transmitted(RiskScoreMsg msg) {
    RiskScore transmitted = msg.score().withValue(msg.value() * params.transRate());
    return RiskScoreMsg.builder().sender(self).score(transmitted).id(msg.id()).build();
  }

  public RiskScoreMsg defaultMsg() {
    return RiskScoreMsg.builder().score(RiskScore.MIN).sender(self).build();
  }

  public boolean isGreaterThan(TemporalProbability msg1, TemporalProbability msg2) {
    return isGreaterThan(msg1, msg2.value());
  }

  public boolean isGreaterThan(TemporalProbability msg, float value) {
    return msg.value() > value;
  }

  public Duration computeScoreTtl(TemporalProbability msg) {
    return computeTtl(msg.time(), params.scoreTtl());
  }

  private Duration computeTtl(Temporal temporal, Duration ttl) {
    return ttl.minus(since(temporal));
  }

  private Duration since(Temporal temporal) {
    return Duration.between(temporal, clock.instant());
  }

  public Duration computeContactTtl(Temporal contactTime) {
    return computeTtl(contactTime, params.contactTtl());
  }

  public boolean isScoreAlive(TemporalProbability msg) {
    return isAlive(msg.time(), params.scoreTtl());
  }

  private boolean isAlive(Temporal temporal, Duration ttl) {
    return since(temporal).compareTo(ttl) < 0;
  }

  public boolean isContactAlive(ContactMsg msg) {
    return isContactAlive(msg.contactTime());
  }

  public boolean isContactAlive(Temporal contactTime) {
    return isAlive(contactTime, params.contactTtl());
  }
}
