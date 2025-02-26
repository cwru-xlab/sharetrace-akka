package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import com.google.common.collect.Range;
import java.io.Flushable;
import java.util.Optional;
import sharetrace.model.Expirable;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.Timestamped;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;

final class Contact implements Expirable, Timestamped, Flushable {

  private int id;
  private ActorRef<UserMessage> ref;
  private long timestamp;
  private long expiryTime;
  private Range<Long> relevantTimeRange;
  private double sendCoefficient;
  private double tolerance;
  private TimeFactory timeFactory;
  private TemporalScore sendThreshold;
  private RiskScoreMessage buffered;

  public Contact(ContactMessage message, Parameters parameters, TimeFactory timeFactory) {
    this.id = message.id();
    this.ref = message.contact();
    this.timestamp = message.timestamp();
    this.expiryTime = message.expiryTime();
    this.relevantTimeRange = Range.lessThan(message.timestamp() + parameters.timeBuffer());
    this.sendCoefficient = parameters.sendCoefficient();
    this.tolerance = parameters.tolerance();
    this.timeFactory = timeFactory;
    resetThreshold();
  }

  private Contact() {}

  public static Contact merge(Contact oldValue, Contact newValue) {
    if (oldValue.id != newValue.id) {
      throw new IllegalArgumentException("Contact identifiers must be the same");
    }
    var result = new Contact();
    var newer = newValue.timestamp > oldValue.timestamp ? newValue : oldValue;
    result.id = newer.id;
    result.ref = newer.ref;
    result.timestamp = newer.timestamp;
    result.expiryTime = newer.expiryTime;
    result.relevantTimeRange = newer.relevantTimeRange;
    result.sendCoefficient = oldValue.sendCoefficient;
    result.tolerance = oldValue.tolerance;
    result.timeFactory = oldValue.timeFactory;
    result.sendThreshold = oldValue.sendThreshold;
    result.buffered = oldValue.buffered;
    return result;
  }

  public void apply(RiskScoreMessage message, RiskScoreMessageStore store) {
    refreshThreshold(store);
    if (isApplicable(message)) {
      setThreshold(message);
      buffered = message;
    }
  }

  public void apply(RiskScoreMessageStore store) {
    maxRelevantMessage(store).ifPresent(message -> apply(message, store));
  }

  @Override
  public void flush() {
    if (buffered != null) {
      ref.tell(buffered);
      buffered = null;
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

  private boolean isApplicable(RiskScoreMessage message) {
    return message.value() > sendThreshold.value()
        && relevantTimeRange.contains(message.timestamp());
  }

  private void refreshThreshold(RiskScoreMessageStore store) {
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
    if (sendThreshold != RiskScore.MIN && sendThreshold.isExpired(timeFactory.getTime())) {
      maxRelevantMessage(store).ifPresentOrElse(this::setThreshold, this::resetThreshold);
    }
  }

  private Optional<RiskScoreMessage> maxRelevantMessage(RiskScoreMessageStore store) {
    return store.max(relevantTimeRange);
  }

  private void setThreshold(RiskScoreMessage message) {
    sendThreshold = message.score().mapValue(value -> value * sendCoefficient + tolerance);
  }

  private void resetThreshold() {
    sendThreshold = RiskScore.MIN;
  }
}
