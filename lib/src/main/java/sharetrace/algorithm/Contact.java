package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import java.time.InstantSource;
import java.util.function.BiConsumer;
import sharetrace.cache.Cache;
import sharetrace.model.Expirable;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.Timestamped;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;

final class Contact implements Expirable, Timestamped, Comparable<Contact> {

  private final ActorRef<UserMessage> self;
  private final Instant timestamp;
  private final Instant bufferedTimestamp;
  private final Instant expiresAt;
  private final double sendCoefficient;
  private final Cache<RiskScoreMessage> scores;
  private final InstantSource timeSource;
  private TemporalScore sendThreshold;

  public Contact(
      ContactMessage message,
      Parameters parameters,
      Cache<RiskScoreMessage> scores,
      InstantSource timeSource) {
    this.self = message.contact();
    this.timestamp = message.timestamp();
    this.bufferedTimestamp = message.timestamp().plus(parameters.timeBuffer());
    this.expiresAt = message.expiresAt();
    this.sendCoefficient = parameters.sendCoefficient();
    this.scores = scores;
    this.timeSource = timeSource;
    resetThreshold();
  }

  public void tell(RiskScoreMessage message, BiConsumer<ActorRef<?>, RiskScoreMessage> logEvent) {
    if (tell(message)) logEvent.accept(self, message);
  }

  public boolean tell(RiskScoreMessage message) {
    refreshThreshold();
    var shouldReceive = shouldReceive(message);
    if (shouldReceive) {
      self.tell(message);
      setThreshold(message.score());
    }
    return shouldReceive;
  }

  @Override
  public Instant expiresAt() {
    return expiresAt;
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  public Instant bufferedTimestamp() {
    return bufferedTimestamp;
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
        && message.timestamp().isBefore(bufferedTimestamp)
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
    if (sendThreshold != RiskScore.MIN && sendThreshold.isExpired(timeSource))
      scores
          .refresh()
          .max(bufferedTimestamp)
          .map(RiskScoreMessage::score)
          .ifPresentOrElse(this::setThreshold, this::resetThreshold);
  }

  private void setThreshold(RiskScore score) {
    sendThreshold = score.mapValue(value -> value * sendCoefficient);
  }

  private void resetThreshold() {
    sendThreshold = RiskScore.MIN;
  }
}
