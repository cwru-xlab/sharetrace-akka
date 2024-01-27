package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import java.time.InstantSource;
import java.util.Optional;
import sharetrace.model.Expirable;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;

final class Contact implements Expirable, Comparable<Contact> {

  private final int id;
  private final ActorRef<UserMessage> ref;
  private final long timestamp;
  private final long bufferedTimestamp;
  private final long expiryTime;
  private final Parameters parameters;
  private final InstantSource timeSource;

  private TemporalScore sendThreshold;
  private RiskScoreMessage sent;

  public Contact(ContactMessage message, Parameters parameters, InstantSource timeSource) {
    this.id = message.id();
    this.ref = message.contact();
    this.timestamp = message.timestamp();
    this.bufferedTimestamp = Math.addExact(message.timestamp(), parameters.timeBuffer());
    this.expiryTime = message.expiryTime();
    this.parameters = parameters;
    this.timeSource = timeSource;
    resetThreshold();
  }

  public void apply(RiskScoreMessage message, Cache<RiskScoreMessage> cache) {
    refreshThreshold(cache);
    if (shouldReceive(message)) {
      sent = message;
      setThreshold(message);
    }
  }

  public void applyCached(Cache<RiskScoreMessage> cache) {
    maxRelevantMessage(cache).ifPresent(message -> apply(message, cache));
  }

  public void flush() {
    if (sent != null) {
      ref.tell(sent);
      sent = null;
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

  public int id() {
    return id;
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public int compareTo(Contact contact) {
    return Expirable.compare(this, contact);
  }

  private boolean shouldReceive(RiskScoreMessage message) {
    return message.value() > sendThreshold.value()
        && message.timestamp() < bufferedTimestamp
        && message.sender() != id;
  }

  private void refreshThreshold(Cache<RiskScoreMessage> cache) {
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
      maxRelevantMessage(cache).ifPresentOrElse(this::setThreshold, this::resetThreshold);
    }
  }

  private Optional<RiskScoreMessage> maxRelevantMessage(Cache<RiskScoreMessage> cache) {
    return cache.refresh().max(bufferedTimestamp);
  }

  private void setThreshold(RiskScoreMessage message) {
    sendThreshold = message.score().mapValue(value -> value * parameters.sendCoefficient());
  }

  private void resetThreshold() {
    sendThreshold = RiskScore.MIN;
  }
}
