package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.message.ContactMsg;
import io.sharetrace.model.message.RiskScoreMsg;
import io.sharetrace.model.message.ThresholdMsg;
import io.sharetrace.model.message.UserMsg;
import io.sharetrace.util.IntervalCache;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

final class ContactActor implements Comparable<ContactActor> {

  private final ActorRef<UserMsg> ref;
  private final Instant contactTime;
  private final Instant bufferedContactTime;
  private final ThresholdMsg thresholdMsg;
  private final IntervalCache<RiskScoreMsg> cache;
  private final MsgUtil msgUtil;
  private final TimerScheduler<UserMsg> timers;
  private float sendThreshold;

  public ContactActor(
      ContactMsg msg,
      TimerScheduler<UserMsg> timers,
      MsgUtil msgUtil,
      IntervalCache<RiskScoreMsg> cache) {
    this.ref = msg.contact();
    this.contactTime = msg.contactTime();
    this.bufferedContactTime = msgUtil.buffered(contactTime);
    this.thresholdMsg = ThresholdMsg.of(ref);
    this.cache = cache;
    this.msgUtil = msgUtil;
    this.timers = timers;
    setThresholdAsDefault();
  }

  private void setThresholdAsDefault() {
    sendThreshold = RiskScore.MIN_VALUE;
  }

  public boolean shouldReceive(RiskScoreMsg msg) {
    // Evaluated in ascending order of likelihood that they are true to possibly short circuit.
    return isAboveThreshold(msg)
        && isRelevant(msg)
        && msgUtil.isScoreAlive(msg)
        && isNotSender(msg);
  }

  private boolean isAboveThreshold(RiskScoreMsg msg) {
    return msgUtil.isGreaterThan(msg, sendThreshold);
  }

  private boolean isRelevant(RiskScoreMsg msg) {
    return msgUtil.isNotAfter(msg, bufferedContactTime);
  }

  private boolean isNotSender(RiskScoreMsg msg) {
    return !ref.equals(msg.sender());
  }

  public Instant bufferedContactTime() {
    return bufferedContactTime;
  }

  public void tell(RiskScoreMsg msg, BiConsumer<ActorRef<?>, RiskScoreMsg> logEvent) {
    ref.tell(msg);
    logEvent.accept(ref, msg);
    updateThreshold(msg);
  }

  private void updateThreshold(RiskScoreMsg msg) {
    float threshold = msgUtil.computeThreshold(msg);
    if (threshold > sendThreshold) {
      sendThreshold = threshold;
      startThresholdTimer(msg);
    }
  }

  private void startThresholdTimer(RiskScoreMsg msg) {
    timers.startSingleTimer(thresholdMsg, msgUtil.computeScoreTtl(msg));
  }

  public void updateThreshold() {
    cache
        .max(bufferedContactTime)
        .filter(msgUtil::isScoreAlive)
        .ifPresentOrElse(this::setThreshold, this::setThresholdAsDefault);
  }

  private void setThreshold(RiskScoreMsg msg) {
    sendThreshold = msgUtil.computeThreshold(msg);
    startThresholdTimer(msg);
  }

  public ActorRef<UserMsg> ref() {
    return ref;
  }

  @Override
  public int compareTo(ContactActor contact) {
    return contactTime.compareTo(contact.contactTime);
  }

  public Duration ttl() {
    return msgUtil.computeContactTtl(contactTime);
  }

  public boolean isAlive() {
    return msgUtil.isContactAlive(contactTime);
  }
}
