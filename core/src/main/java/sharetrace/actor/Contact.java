package sharetrace.actor;

import akka.actor.typed.ActorRef;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import org.immutables.builder.Builder;
import sharetrace.model.Expirable;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.RangeCache;

final class Contact implements Expirable, Comparable<Contact> {

  private final ActorRef<UserMessage> self;
  private final Duration expiry;
  private final Instant timestamp;
  private final Instant bufferedTimestamp;
  private final float sendCoefficient;
  private final RangeCache<RiskScoreMessage> scores;
  private final Clock clock;

  private RiskScore sendThreshold;

  @Builder.Constructor
  Contact(
      ContactMessage message,
      Parameters parameters,
      RangeCache<RiskScoreMessage> scores,
      Clock clock) {
    this.self = message.contact();
    this.expiry = message.expiry();
    this.timestamp = message.timestamp();
    this.bufferedTimestamp = timestamp.plus(parameters.timeBuffer());
    this.sendCoefficient = parameters.sendCoefficient();
    this.scores = scores;
    this.clock = clock;
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
      setThreshold(message.score());
    }
  }

  private void refreshThreshold() {
    /* When sendThreshold is the default score, it indicates that either no message has yet been
    sent to this contact and/or the score cache is empty. If the cache is empty, and we have not yet
    sent a message to this contact, then the message retrieved from the cache will be the same
    message we are trying to send to the contact. Thus, no message will be sent since the message
    is not greater than its own value. If we have sent a message to the contact, but it has since
    expired, then updating the threshold based on the cache has no effect; we might as well just
    wait for the next message the contact should receive, send that, and then update the threshold.
    */
    if (sendThreshold != RiskScore.MIN && sendThreshold.isExpired(clock)) {
      scores
          .refresh()
          .max(bufferedTimestamp)
          .map(RiskScoreMessage::score)
          .ifPresentOrElse(this::setThreshold, this::resetThreshold);
    }
  }

  @Override
  public Duration expiry() {
    return expiry;
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
    if (obj instanceof Contact) {
      Contact contact = (Contact) obj;
      return self.equals(contact.self);
    }
    return false;
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

  private void setThreshold(RiskScore score) {
    sendThreshold = score.mapValue(value -> value * sendCoefficient);
  }

  private void resetThreshold() {
    sendThreshold = RiskScore.MIN;
  }
}
