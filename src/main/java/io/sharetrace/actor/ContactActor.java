package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.ThresholdMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.RiskScore;
import io.sharetrace.util.IntervalCache;
import java.time.Instant;
import java.util.function.BiConsumer;

final class ContactActor {

  private final ActorRef<UserMsg> ref;
  private final Instant contactTime;
  private final IntervalCache<RiskScoreMsg> cache;
  private final MsgUtil msgUtil;
  private final TimerScheduler<UserMsg> timers;
  private final RiskScoreMsg defaultMsg;
  private RiskScoreMsg thresholdMsg;
  private float sendThreshold;

  public ContactActor(
      ContactMsg msg,
      TimerScheduler<UserMsg> timers,
      MsgUtil msgUtil,
      IntervalCache<RiskScoreMsg> cache,
      RiskScoreMsg defaultMsg) {
    this.ref = msg.contact();
    this.contactTime = msg.contactTime();
    this.cache = cache;
    this.msgUtil = msgUtil;
    this.timers = timers;
    this.defaultMsg = defaultMsg;
    this.thresholdMsg = defaultMsg;
    this.sendThreshold = RiskScore.MIN_VALUE;
  }

  public boolean shouldReceive(RiskScoreMsg msg) {
    return exceedsThreshold(msg) && isRelevant(msg) && msgUtil.isAlive(msg) && isNotSender(msg);
  }

  public Instant bufferedContactTime() {
    return msgUtil.buffered(contactTime);
  }

  public void tell(RiskScoreMsg msg, BiConsumer<ActorRef<?>, RiskScoreMsg> logEvent) {
    ref.tell(msg);
    logEvent.accept(ref, msg);
    updateThreshold(msg);
  }

  public void updateThreshold() {
    thresholdMsg = cache.max(bufferedContactTime()).filter(msgUtil::isAlive).orElse(defaultMsg);
    sendThreshold = msgUtil.computeThreshold(thresholdMsg);
  }

  public ActorRef<UserMsg> ref() {
    return ref;
  }

  private boolean exceedsThreshold(RiskScoreMsg msg) {
    return msgUtil.isGreaterThan(msg, sendThreshold);
  }

  private boolean isRelevant(RiskScoreMsg msg) {
    return msgUtil.isNotAfter(msg, bufferedContactTime());
  }

  private boolean isNotSender(RiskScoreMsg msg) {
    return !ref.equals(msg.replyTo());
  }

  private void updateThreshold(RiskScoreMsg msg) {
    float threshold = msgUtil.computeThreshold(msg);
    if (threshold > sendThreshold) {
      sendThreshold = threshold;
      thresholdMsg = msg;
      timers.startSingleTimer(ThresholdMsg.of(ref), msgUtil.computeTtl(thresholdMsg));
    }
  }

  public boolean isAlive() {
    return msgUtil.isAlive(contactTime);
  }
}
