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
    this.cache = cache;
    this.helper = helper;
    this.timers = timers;
    this.self = message.contact();
    this.timestamp = message.timestamp();
    this.bufferedTimestamp = helper.buffered(timestamp);
    this.thresholdMessage = ThresholdMessage.of(self);
    resetThreshold();
  }

  public boolean shouldReceive(RiskScoreMessage message) {
    return helper.isAbove(message, sendThreshold)
        && message.timestamp().isBefore(bufferedTimestamp)
        && !helper.isExpired(message)
        && !self.equals(message.sender());
  }

  public void tell(RiskScoreMessage message, BiConsumer<ActorRef<?>, RiskScoreMessage> logEvent) {
    self.tell(message);
    logEvent.accept(self, message);
    setThresholdAndStartTimer(message);
  }

  public void updateThreshold() {
    cache
        .max(bufferedTimestamp)
        .filter(Predicate.not(helper::isExpired))
        .ifPresentOrElse(this::setThresholdAndStartTimer, this::resetThreshold);
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

  public Instant bufferedTimestamp() {
    return bufferedTimestamp;
  }

  @Override
  public int compareTo(Contact contact) {
    return timestamp.compareTo(contact.timestamp);
  }

  private void setThresholdAndStartTimer(TemporalScore score) {
    sendThreshold = helper.threshold(score);
    timers.startSingleTimer(thresholdMessage, helper.untilExpiry(score));
  }

  private void resetThreshold() {
    sendThreshold = RiskScore.MIN_VALUE;
  }
}
