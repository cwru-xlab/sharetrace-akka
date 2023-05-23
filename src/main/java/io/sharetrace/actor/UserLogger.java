package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import io.sharetrace.model.message.RiskScoreMsg;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.TypedLogger;
import io.sharetrace.util.logging.event.ContactEvent;
import io.sharetrace.util.logging.event.ContactsRefreshEvent;
import io.sharetrace.util.logging.event.CurrentRefreshEvent;
import io.sharetrace.util.logging.event.LoggableEvent;
import io.sharetrace.util.logging.event.PropagateEvent;
import io.sharetrace.util.logging.event.ReceiveEvent;
import io.sharetrace.util.logging.event.SendCachedEvent;
import io.sharetrace.util.logging.event.SendCurrentEvent;
import io.sharetrace.util.logging.event.UpdateEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

final class UserLogger {

  private static final TypedLogger<LoggableEvent> LOGGER = Logging.eventsLogger();
  private final String self;
  private final Clock clock;

  public UserLogger(ActorRef<?> self, Clock clock) {
    this.self = name(self);
    this.clock = clock;
  }

  private static String name(ActorRef<?> user) {
    return user.path().name();
  }

  private static <T extends LoggableEvent> void logEvent(Class<T> type, Supplier<T> event) {
    LOGGER.log(LoggableEvent.KEY, type, event);
  }

  public void logContactEvent(ActorRef<?> contact) {
    logEvent(ContactEvent.class, () -> contactEvent(contact));
  }

  private ContactEvent contactEvent(ActorRef<?> contact) {
    return ContactEvent.builder().self(self).contact(name(contact)).timestamp(timestamp()).build();
  }

  public void logSendCachedEvent(ActorRef<?> receiver, RiskScoreMsg cached) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(receiver, cached));
  }

  private SendCachedEvent sendCachedEvent(ActorRef<?> receiver, RiskScoreMsg cached) {
    return SendCachedEvent.builder()
        .self(self)
        .message(cached)
        .receiver(name(receiver))
        .timestamp(timestamp())
        .build();
  }

  public void logSendCurrentEvent(ActorRef<?> receiver, RiskScoreMsg riskScore) {
    logEvent(SendCurrentEvent.class, () -> sendCurrentEvent(receiver, riskScore));
  }

  private SendCurrentEvent sendCurrentEvent(ActorRef<?> receiver, RiskScoreMsg current) {
    return SendCurrentEvent.builder()
        .self(self)
        .message(current)
        .receiver(name(receiver))
        .timestamp(timestamp())
        .build();
  }

  public void logReceiveEvent(RiskScoreMsg received) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(received));
  }

  private ReceiveEvent receiveEvent(RiskScoreMsg received) {
    return ReceiveEvent.builder()
        .self(self)
        .sender(name(received.sender()))
        .message(received)
        .timestamp(timestamp())
        .build();
  }

  public void logUpdateEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    logEvent(UpdateEvent.class, () -> updateEvent(previous, current));
  }

  private UpdateEvent updateEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    return UpdateEvent.builder()
        .self(self)
        .previous(previous)
        .current(current)
        .timestamp(timestamp())
        .build();
  }

  public void logPropagateEvent(ActorRef<?> receiver, RiskScoreMsg propagated) {
    logEvent(PropagateEvent.class, () -> propagateEvent(receiver, propagated));
  }

  private PropagateEvent propagateEvent(ActorRef<?> receiver, RiskScoreMsg propagated) {
    return PropagateEvent.builder()
        .self(self)
        .message(propagated)
        .receiver(name(receiver))
        .timestamp(timestamp())
        .build();
  }

  public void logContactsRefreshEvent(int remaining, int expired) {
    logEvent(ContactsRefreshEvent.class, () -> contactsRefreshEvent(remaining, expired));
  }

  private ContactsRefreshEvent contactsRefreshEvent(int remaining, int expired) {
    return ContactsRefreshEvent.builder()
        .self(self)
        .remaining(remaining)
        .expired(expired)
        .timestamp(timestamp())
        .build();
  }

  public void logCurrentRefreshEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    logEvent(CurrentRefreshEvent.class, () -> currentRefreshEvent(previous, current));
  }

  private CurrentRefreshEvent currentRefreshEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    return CurrentRefreshEvent.builder()
        .self(self)
        .previous(previous)
        .current(current)
        .timestamp(timestamp())
        .build();
  }

  private Instant timestamp() {
    return clock.instant();
  }
}
