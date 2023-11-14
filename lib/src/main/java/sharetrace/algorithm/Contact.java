package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import java.util.Optional;
import java.util.function.BiConsumer;
import sharetrace.cache.Cache;
import sharetrace.model.Expirable;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.Timestamp;
import sharetrace.model.Timestamped;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.TimeSource;

final class Contact implements Expirable, Timestamped, Comparable<Contact> {

  private final ActorRef<UserMessage> self;
  private final Timestamp timestamp;
  private final Timestamp bufferedTimestamp;
  private final Timestamp expiryTime;
  private final double sendCoefficient;
  private final Cache<RiskScoreMessage> scores;
  private final TimeSource timeSource;
  private TemporalScore sendThreshold;

  public Contact(
      ContactMessage message,
      Parameters parameters,
      Cache<RiskScoreMessage> scores,
      TimeSource timeSource) {
    this.self = message.contact();
    this.timestamp = message.timestamp();
    this.bufferedTimestamp = message.timestamp().plus(parameters.timeBuffer());
    this.expiryTime = message.expiryTime();
    this.sendCoefficient = parameters.sendCoefficient();
    this.scores = scores;
    this.timeSource = timeSource;
    resetThreshold();
  }

  public void tell(RiskScoreMessage message) {
    tell(message, (self, msg) -> {});
  }

  public void tell(RiskScoreMessage message, BiConsumer<ActorRef<?>, RiskScoreMessage> logEvent) {
    refreshThreshold();
    if (shouldReceive(message)) {
      self.tell(message);
      logEvent.accept(self, message);
      setThreshold(message);
    }
  }

  public Optional<RiskScoreMessage> maxRelevantMessageInCache() {
    return scores.refresh().max(bufferedTimestamp);
  }

  @Override
  public Timestamp expiryTime() {
    return expiryTime;
  }

  @Override
  public Timestamp timestamp() {
    return timestamp;
  }

  public ActorRef<UserMessage> self() {
    return self;
  }

  @Override
  public int hashCode() {
    return self.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Contact contact && self.equals(contact.self);
  }

  @Override
  public int compareTo(Contact contact) {
    return timestamp.compareTo(contact.timestamp);
  }

  private boolean shouldReceive(RiskScoreMessage message) {
    return message.value() > sendThreshold.value()
        && message.timestamp().before(bufferedTimestamp)
        && !message.sender().equals(self);
  }

  private void refreshThreshold() {
    /*
     If this contact's send threshold is the minimum risk score, then either
       1. no messages have been sent to this contact; or
       2. the threshold has expired and, the cache was empty upon previously refreshing.
     In either case, refreshing the cache before sending the message may result in setting the
     threshold based on a cached message whose value is at least the value of the message about to
     be sent. In this situation, the message would not be sent to this contact since messages only
     with a value *greater* than the threshold are eligible. Considering this in the context of the
     entire contact network, this would prevent all propagation of messages.
    */
    if (sendThreshold != RiskScore.MIN && sendThreshold.isExpired(timeSource.timestamp())) {
      maxRelevantMessageInCache().ifPresentOrElse(this::setThreshold, this::resetThreshold);
    }
  }

  private void setThreshold(RiskScoreMessage message) {
    sendThreshold = message.score().mapValue(value -> value * sendCoefficient);
  }

  private void resetThreshold() {
    sendThreshold = RiskScore.MIN;
  }
}
