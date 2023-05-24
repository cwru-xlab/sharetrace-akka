package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import io.sharetrace.model.message.RiskScoreMessage;
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

  public void logSendCachedEvent(ActorRef<?> contact, RiskScoreMessage cached) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(contact, cached));
  }

  private SendCachedEvent sendCachedEvent(ActorRef<?> contact, RiskScoreMessage cached) {
    return SendCachedEvent.builder()
        .self(self)
        .message(cached)
        .contact(name(contact))
        .timestamp(timestamp())
        .build();
  }

  public void logSendCurrentEvent(ActorRef<?> contact, RiskScoreMessage current) {
    logEvent(SendCurrentEvent.class, () -> sendCurrentEvent(contact, current));
  }

  private SendCurrentEvent sendCurrentEvent(ActorRef<?> contact, RiskScoreMessage current) {
    return SendCurrentEvent.builder()
        .self(self)
        .message(current)
        .contact(name(contact))
        .timestamp(timestamp())
        .build();
  }

  public void logReceiveEvent(RiskScoreMessage received) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(received));
  }

  private ReceiveEvent receiveEvent(RiskScoreMessage received) {
    return ReceiveEvent.builder()
        .self(self)
        .contact(name(received.sender()))
        .message(received)
        .timestamp(timestamp())
        .build();
  }

  public void logUpdateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    logEvent(UpdateEvent.class, () -> updateEvent(previous, current));
  }

  private UpdateEvent updateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    return UpdateEvent.builder()
        .self(self)
        .previous(previous)
        .current(current)
        .timestamp(timestamp())
        .build();
  }

  public void logPropagateEvent(ActorRef<?> contact, RiskScoreMessage propagated) {
    logEvent(PropagateEvent.class, () -> propagateEvent(contact, propagated));
  }

  private PropagateEvent propagateEvent(ActorRef<?> contact, RiskScoreMessage propagated) {
    return PropagateEvent.builder()
        .self(self)
        .message(propagated)
        .contact(name(contact))
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

  public void logCurrentRefreshEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    logEvent(CurrentRefreshEvent.class, () -> currentRefreshEvent(previous, current));
  }

  private CurrentRefreshEvent currentRefreshEvent(
      RiskScoreMessage previous, RiskScoreMessage current) {
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
