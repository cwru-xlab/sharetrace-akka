package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import io.sharetrace.model.RiskScore;
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

  public boolean isNotAfter(RiskScoreMsg msg, Instant time) {
    return !msg.score().time().isAfter(time);
  }

  public float computeThreshold(RiskScoreMsg msg) {
    return msg.score().value() * params.sendCoeff();
  }

  public Instant buffered(Instant instant) {
    return instant.plus(params.timeBuffer());
  }

  public RiskScoreMsg transmitted(RiskScoreMsg msg) {
    RiskScore original = msg.score();
    RiskScore transmitted = original.withValue(original.value() * params.transRate());
    return RiskScoreMsg.builder().sender(self).score(transmitted).id(msg.id()).build();
  }

  public RiskScoreMsg defaultMsg() {
    return RiskScoreMsg.builder().score(RiskScore.MIN).sender(self).build();
  }

  public boolean isGreaterThan(RiskScoreMsg msg1, RiskScoreMsg msg2) {
    return isGreaterThan(msg1, msg2.score().value());
  }

  public boolean isGreaterThan(RiskScoreMsg msg, float value) {
    return msg.score().value() > value;
  }

  public Duration computeScoreTtl(RiskScoreMsg msg) {
    return computeTtl(msg.score().time(), params.scoreTtl());
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

  public boolean isScoreAlive(RiskScoreMsg msg) {
    return isAlive(msg.score().time(), params.scoreTtl());
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
