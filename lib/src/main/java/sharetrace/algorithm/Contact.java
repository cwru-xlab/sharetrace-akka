package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import java.time.InstantSource;
import java.util.Optional;
import java.util.function.BiConsumer;
import sharetrace.model.Expirable;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Cache;

final class Contact implements Expirable, Comparable<Contact> {

  private final ActorRef<UserMessage> self;
  private final long timestamp;
  private final long bufferedTimestamp;
  private final long expiryTime;
  private final Parameters parameters;
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
    this.bufferedTimestamp = Math.addExact(message.timestamp(), parameters.timeBuffer());
    this.expiryTime = message.expiryTime();
    this.parameters = parameters;
    this.scores = scores;
    this.timeSource = timeSource;
    resetThreshold();
  }

  public void tell(RiskScoreMessage message) {
    tell(message, (self, msg) -> {});
  }

  public void tellInitialMessage(BiConsumer<ActorRef<?>, RiskScoreMessage> logEvent) {
    /* Always try to send a new contact a risk score. An expired contact may still receive a risk
    score if it is "relevant" (i.e., within the time buffer of the contact time). */
    maxRelevantMessageInCache().ifPresent(msg -> tell(msg, logEvent));
  }

  public void tell(RiskScoreMessage message, BiConsumer<ActorRef<?>, RiskScoreMessage> logEvent) {
    refreshThreshold();
    if (shouldReceive(message)) {
      self.tell(message);
      logEvent.accept(self, message);
      setThreshold(message);
    }
  }

  @Override
  public long expiryTime() {
    return expiryTime;
  }

  @Override
  public long timestamp() {
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
  @SuppressWarnings("NullableProblems")
  public int compareTo(Contact contact) {
    return Expirable.compare(this, contact);
  }

  private boolean shouldReceive(RiskScoreMessage message) {
    return message.value() > sendThreshold.value()
        && message.timestamp() < bufferedTimestamp
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
    if (sendThreshold != RiskScore.MIN && sendThreshold.isExpired(timeSource.millis())) {
      maxRelevantMessageInCache().ifPresentOrElse(this::setThreshold, this::resetThreshold);
    }
  }

  private Optional<RiskScoreMessage> maxRelevantMessageInCache() {
    return scores.refresh().max(bufferedTimestamp);
  }

  private void setThreshold(RiskScoreMessage message) {
    sendThreshold = message.score().mapValue(value -> value * parameters.sendCoefficient());
  }

  private void resetThreshold() {
    sendThreshold = RiskScore.MIN;
  }
}
