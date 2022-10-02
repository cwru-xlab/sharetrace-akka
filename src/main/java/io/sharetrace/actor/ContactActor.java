package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.ThresholdMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.RiskScore;
import io.sharetrace.util.IntervalCache;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

final class ContactActor implements Comparable<ContactActor> {

  private static final float DEFAULT_THRESHOLD = RiskScore.MIN_VALUE;
  private final ActorRef<UserMsg> ref;
  private final Instant contactTime;
  private final Instant bufferedContactTime;
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
    this.cache = cache;
    this.msgUtil = msgUtil;
    this.timers = timers;
    this.sendThreshold = DEFAULT_THRESHOLD;
  }

  public boolean shouldReceive(RiskScoreMsg msg) {
    return isAboveThreshold(msg) && isRelevant(msg) && msgUtil.isAlive(msg) && isNotSender(msg);
  }

  private boolean isAboveThreshold(RiskScoreMsg msg) {
    return msgUtil.isGreaterThan(msg, sendThreshold);
  }

  private boolean isRelevant(RiskScoreMsg msg) {
    return msgUtil.isNotAfter(msg, bufferedContactTime);
  }

  private boolean isNotSender(RiskScoreMsg msg) {
    return !ref.equals(msg.replyTo());
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
    timers.startSingleTimer(ThresholdMsg.of(ref), msgUtil.computeTtl(msg));
  }

  public void updateThreshold() {
    cache
        .max(bufferedContactTime)
        .filter(msgUtil::isAlive)
        .ifPresentOrElse(this::setThreshold, this::setThresholdAsDefault);
  }

  private void setThreshold(RiskScoreMsg msg) {
    sendThreshold = msgUtil.computeThreshold(msg);
    startThresholdTimer(msg);
  }

  private void setThresholdAsDefault() {
    sendThreshold = DEFAULT_THRESHOLD;
  }

  public ActorRef<UserMsg> ref() {
    return ref;
  }

  @Override
  public int compareTo(ContactActor contact) {
    return contactTime.compareTo(contact.contactTime);
  }

  public Duration ttl() {
    return msgUtil.computeTtl(contactTime);
  }

  public boolean isAlive() {
    return msgUtil.isAlive(contactTime);
  }
}
