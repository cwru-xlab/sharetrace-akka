package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.TemporalScore;
import io.sharetrace.model.message.ContactMessage;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.model.message.ThresholdMessage;
import io.sharetrace.model.message.UserMessage;
import io.sharetrace.util.cache.IntervalCache;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

final class ContactActor implements Comparable<ContactActor> {

  private final ActorRef<UserMessage> ref;
  private final Instant contactTime;
  private final Instant bufferedContactTime;
  private final ThresholdMessage thresholdMessage;
  private final IntervalCache<? extends TemporalScore> cache;
  private final MsgUtil msgUtil;
  private final TimerScheduler<UserMessage> timers;
  private float sendThreshold;

  public ContactActor(
      ContactMessage message,
      TimerScheduler<UserMessage> timers,
      MsgUtil msgUtil,
      IntervalCache<? extends TemporalScore> cache) {
    this.ref = message.contact();
    this.contactTime = message.contactTime();
    this.bufferedContactTime = msgUtil.buffered(contactTime);
    this.thresholdMessage = ThresholdMessage.of(ref);
    this.cache = cache;
    this.msgUtil = msgUtil;
    this.timers = timers;
    setThresholdAsDefault();
  }

  private void setThresholdAsDefault() {
    sendThreshold = RiskScore.MIN_VALUE;
  }

  public boolean shouldReceive(RiskScoreMessage message) {
    return isAboveThreshold(message)
        && isRelevant(message)
        && msgUtil.isScoreAlive(message)
        && isNotSender(message);
  }

  private boolean isAboveThreshold(TemporalScore score) {
    return score.value() > sendThreshold;
  }

  private boolean isRelevant(TemporalScore score) {
    return !score.timestamp().isAfter(bufferedContactTime);
  }

  private boolean isNotSender(RiskScoreMessage message) {
    return !ref.equals(message.sender());
  }

  public Instant bufferedContactTime() {
    return bufferedContactTime;
  }

  public void tell(RiskScoreMessage message, BiConsumer<ActorRef<?>, RiskScoreMessage> logEvent) {
    ref.tell(message);
    logEvent.accept(ref, message);
    updateThreshold(message);
  }

  private void updateThreshold(TemporalScore score) {
    float threshold = msgUtil.threshold(score);
    if (threshold > sendThreshold) {
      sendThreshold = threshold;
      startThresholdTimer(score);
    }
  }

  private void startThresholdTimer(TemporalScore score) {
    timers.startSingleTimer(thresholdMessage, msgUtil.scoreTimeToLive(score));
  }

  public void updateThreshold() {
    cache
        .max(bufferedContactTime)
        .filter(msgUtil::isScoreAlive)
        .ifPresentOrElse(this::setThreshold, this::setThresholdAsDefault);
  }

  private void setThreshold(TemporalScore score) {
    sendThreshold = msgUtil.threshold(score);
    startThresholdTimer(score);
  }

  public ActorRef<UserMessage> ref() {
    return ref;
  }

  @Override
  public int compareTo(ContactActor contact) {
    return contactTime.compareTo(contact.contactTime);
  }

  public Duration timeToLive() {
    return msgUtil.contactTimeToLive(contactTime);
  }

  public boolean isAlive() {
    return msgUtil.isContactAlive(contactTime);
  }
}
