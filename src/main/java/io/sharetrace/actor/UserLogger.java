package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.RecordLogger;
import io.sharetrace.util.logging.event.ContactEvent;
import io.sharetrace.util.logging.event.ContactsRefreshEvent;
import io.sharetrace.util.logging.event.CurrentRefreshEvent;
import io.sharetrace.util.logging.event.EventRecord;
import io.sharetrace.util.logging.event.PropagateEvent;
import io.sharetrace.util.logging.event.ReceiveEvent;
import io.sharetrace.util.logging.event.SendCachedEvent;
import io.sharetrace.util.logging.event.SendCurrentEvent;
import io.sharetrace.util.logging.event.UpdateEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

final class UserLogger {

  private static final RecordLogger<EventRecord> LOGGER = Logging.eventsLogger();

  private final String self;
  private final Clock clock;

  public UserLogger(ActorRef<?> self, Clock clock) {
    this.self = name(self);
    this.clock = clock;
  }

  private static String name(ActorRef<?> user) {
    return user.path().name();
  }

  private static <T extends EventRecord> void logEvent(Class<T> type, Supplier<T> event) {
    LOGGER.log(EventRecord.KEY, type, event);
  }

  public void logContactEvent(ActorRef<?> contact) {
    logEvent(ContactEvent.class, () -> contactEvent(contact));
  }

  public void logSendCachedEvent(ActorRef<?> contact, RiskScoreMessage cached) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(contact, cached));
  }

  public void logSendCurrentEvent(ActorRef<?> contact, RiskScoreMessage current) {
    logEvent(SendCurrentEvent.class, () -> sendCurrentEvent(contact, current));
  }

  public void logReceiveEvent(RiskScoreMessage received) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(received));
  }

  public void logUpdateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    logEvent(UpdateEvent.class, () -> updateEvent(previous, current));
  }

  public void logPropagateEvent(ActorRef<?> contact, RiskScoreMessage propagated) {
    logEvent(PropagateEvent.class, () -> propagateEvent(contact, propagated));
  }

  public void logContactsRefreshEvent(int remaining, int expired) {
    logEvent(ContactsRefreshEvent.class, () -> contactsRefreshEvent(remaining, expired));
  }

  public void logCurrentRefreshEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    logEvent(CurrentRefreshEvent.class, () -> currentRefreshEvent(previous, current));
  }

  private ContactEvent contactEvent(ActorRef<?> contact) {
    return ContactEvent.builder().self(self).contact(name(contact)).timestamp(timestamp()).build();
  }

  private SendCachedEvent sendCachedEvent(ActorRef<?> contact, RiskScoreMessage cached) {
    return SendCachedEvent.builder()
        .self(self)
        .message(cached)
        .contact(name(contact))
        .timestamp(timestamp())
        .build();
  }

  private SendCurrentEvent sendCurrentEvent(ActorRef<?> contact, RiskScoreMessage current) {
    return SendCurrentEvent.builder()
        .self(self)
        .message(current)
        .contact(name(contact))
        .timestamp(timestamp())
        .build();
  }

  private ReceiveEvent receiveEvent(RiskScoreMessage received) {
    return ReceiveEvent.builder()
        .self(self)
        .contact(name(received.sender()))
        .message(received)
        .timestamp(timestamp())
        .build();
  }

  private UpdateEvent updateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    return UpdateEvent.builder()
        .self(self)
        .previous(previous)
        .current(current)
        .timestamp(timestamp())
        .build();
  }

  private PropagateEvent propagateEvent(ActorRef<?> contact, RiskScoreMessage propagated) {
    return PropagateEvent.builder()
        .self(self)
        .message(propagated)
        .contact(name(contact))
        .timestamp(timestamp())
        .build();
  }

  private ContactsRefreshEvent contactsRefreshEvent(int remaining, int expired) {
    return ContactsRefreshEvent.builder()
        .self(self)
        .remaining(remaining)
        .expired(expired)
        .timestamp(timestamp())
        .build();
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
