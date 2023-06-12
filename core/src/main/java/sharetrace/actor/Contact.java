package sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.ThresholdMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.cache.IntervalCache;

final class Contact implements Comparable<Contact> {

  private final ActorRef<UserMessage> self;
  private final Instant timestamp;
  private final Instant bufferedTimestamp;
  private final ThresholdMessage thresholdMessage;
  private final IntervalCache<? extends TemporalScore> cache;
  private final UserHelper helper;
  private final TimerScheduler<UserMessage> timers;

  private float sendThreshold;

  public Contact(
      ContactMessage message,
      TimerScheduler<UserMessage> timers,
      UserHelper helper,
      IntervalCache<? extends TemporalScore> cache) {
    this.self = message.contact();
    this.timestamp = message.timestamp();
    this.bufferedTimestamp = helper.buffered(timestamp);
    this.thresholdMessage = ThresholdMessage.of(self);
    this.cache = cache;
    this.helper = helper;
    this.timers = timers;
    setThresholdAsDefault();
  }

  public boolean shouldReceive(RiskScoreMessage message) {
    return message.value() > sendThreshold
        && message.timestamp().isBefore(bufferedTimestamp)
        && !helper.isExpired(message)
        && !self.equals(message.sender());
  }

  public void tell(RiskScoreMessage message, BiConsumer<ActorRef<?>, RiskScoreMessage> logEvent) {
    self.tell(message);
    logEvent.accept(self, message);
    updateThresholdAndStartTimer(message);
  }

  public void updateThresholdAndStartTimer() {
    cache
        .max(bufferedTimestamp)
        .filter(Predicate.not(helper::isExpired))
        .ifPresentOrElse(this::setThresholdAndStartTimer, this::setThresholdAsDefault);
  }

  public Duration untilExpiry() {
    return helper.untilExpiry(timestamp);
  }

  public boolean isExpired() {
    return helper.isExpired(timestamp);
  }

  public ActorRef<UserMessage> reference() {
    return self;
  }

  public Instant bufferedContactTime() {
    return bufferedTimestamp;
  }

  @Override
  public int compareTo(Contact contact) {
    return timestamp.compareTo(contact.timestamp);
  }

  private void updateThresholdAndStartTimer(TemporalScore score) {
    float threshold = helper.threshold(score);
    if (threshold > sendThreshold) {
      sendThreshold = threshold;
      startThresholdTimer(score);
    }
  }

  private void setThresholdAndStartTimer(TemporalScore score) {
    sendThreshold = helper.threshold(score);
    startThresholdTimer(score);
  }

  private void startThresholdTimer(TemporalScore score) {
    timers.startSingleTimer(thresholdMessage, helper.untilExpiry(score));
  }

  private void setThresholdAsDefault() {
    sendThreshold = RiskScore.MIN_VALUE;
  }
}
