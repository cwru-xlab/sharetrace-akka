package io.sharetrace.actor;

import akka.actor.typed.javadsl.ActorContext;
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.UserParams;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;

final class MsgUtil {

  private final ActorContext<UserMsg> ctx;
  private final Clock clock;
  private final UserParams params;

  public MsgUtil(ActorContext<UserMsg> ctx, Clock clock, UserParams params) {
    this.ctx = ctx;
    this.clock = clock;
    this.params = params;
  }

  public boolean isNotAfter(RiskScoreMsg msg, Instant time) {
    return !msg.score().time().isAfter(time);
  }

  public float computeThreshold(RiskScoreMsg msg) {
    return msg.score().value() * params.sendCoeff();
  }

  public Instant buffered(Instant time) {
    return time.plus(params.timeBuffer());
  }

  public RiskScoreMsg transmitted(RiskScoreMsg msg) {
    RiskScore original = msg.score();
    RiskScore transmitted = original.withValue(original.value() * params.transRate());
    return RiskScoreMsg.of(transmitted, ctx.getSelf(), msg.id());
  }

  public RiskScoreMsg defaultMsg() {
    return RiskScoreMsg.of(RiskScore.MIN, ctx.getSelf());
  }

  public boolean isGreaterThan(RiskScoreMsg msg1, RiskScoreMsg msg2) {
    return isGreaterThan(msg1, msg2.score().value());
  }

  public boolean isGreaterThan(RiskScoreMsg msg, float value) {
    return msg.score().value() > value;
  }

  public Duration computeTtl(RiskScoreMsg msg) {
    return computeTtl(msg.score().time(), params.scoreTtl());
  }

  private Duration computeTtl(Temporal temporal, Duration ttl) {
    return ttl.minus(since(temporal));
  }

  private Duration since(Temporal temporal) {
    return Duration.between(temporal, clock.instant());
  }

  public Duration computeTtl(Temporal contactTime) {
    return computeTtl(contactTime, params.contactTtl());
  }

  public boolean isAlive(RiskScoreMsg msg) {
    return isAlive(msg.score().time(), params.scoreTtl());
  }

  private boolean isAlive(Temporal temporal, Duration ttl) {
    return since(temporal).compareTo(ttl) < 0;
  }

  public boolean isAlive(ContactMsg msg) {
    return isAlive(msg.contactTime());
  }

  public boolean isAlive(Temporal contactTime) {
    return isAlive(contactTime, params.contactTtl());
  }
}
