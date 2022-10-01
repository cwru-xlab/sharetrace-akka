package io.sharetrace.actor;

import akka.actor.typed.javadsl.ActorContext;
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.MsgParams;
import io.sharetrace.model.RiskScore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;

final class MsgUtil {

  private final ActorContext<UserMsg> ctx;
  private final Clock clock;
  private final MsgParams params;

  public MsgUtil(ActorContext<UserMsg> ctx, Clock clock, MsgParams params) {
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
    return RiskScoreMsg.builder().score(transmitted).replyTo(ctx.getSelf()).id(msg.id()).build();
  }

  public RiskScoreMsg defaultMsg() {
    return RiskScoreMsg.builder().score(RiskScore.MIN).replyTo(ctx.getSelf()).build();
  }

  public boolean isGreaterThan(RiskScoreMsg msg1, RiskScoreMsg msg2) {
    return isGreaterThan(msg1, msg2.score().value());
  }

  public boolean isGreaterThan(RiskScoreMsg msg, float value) {
    return msg.score().value() > value;
  }

  public Duration remainingTtl(RiskScoreMsg msg) {
    return remainingTtl(msg.score().time(), params.scoreTtl());
  }

  public Duration remainingTtl(Temporal contactTime) {
    return remainingTtl(contactTime, params.contactTtl());
  }

  public boolean isAlive(RiskScoreMsg msg) {
    return isAlive(msg.score().time(), params.scoreTtl());
  }

  public boolean isAlive(ContactMsg msg) {
    return isAlive(msg.contactTime());
  }

  public boolean isAlive(Temporal contactTime) {
    return isAlive(contactTime, params.contactTtl());
  }

  private Duration remainingTtl(Temporal temporal, Duration ttl) {
    return ttl.minus(since(temporal));
  }

  private boolean isAlive(Temporal temporal, Duration ttl) {
    return since(temporal).compareTo(ttl) < 0;
  }

  private Duration since(Temporal temporal) {
    return Duration.between(temporal, clock.instant());
  }
}
