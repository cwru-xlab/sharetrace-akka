package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import sharetrace.model.Expirable;
import sharetrace.model.Timestamped;
import sharetrace.util.Ranges;

public record ContactMessage(ActorRef<UserMessage> contact, Instant timestamp, Instant expiryTime)
    implements Expirable, Timestamped, UserMessage {

  private static final Range<Instant> TIME_RANGE = Range.atLeast(Timestamped.MIN_TIME);

  public ContactMessage {
    Ranges.check("timestamp", timestamp, TIME_RANGE);
    Ranges.check("expiryTime", expiryTime, TIME_RANGE);
  }

  public ContactMessage(ActorRef<UserMessage> contact, Instant timestamp, Duration expiry) {
    this(contact, timestamp, timestamp.plus(expiry));
  }
}
