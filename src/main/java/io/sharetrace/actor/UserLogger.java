package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import io.sharetrace.logging.event.ContactEvent;
import io.sharetrace.logging.event.ContactsRefreshEvent;
import io.sharetrace.logging.event.CurrentRefreshEvent;
import io.sharetrace.logging.event.PropagateEvent;
import io.sharetrace.logging.event.ReceiveEvent;
import io.sharetrace.logging.event.ResumeEvent;
import io.sharetrace.logging.event.SendCachedEvent;
import io.sharetrace.logging.event.SendCurrentEvent;
import io.sharetrace.logging.event.TimeoutEvent;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.util.logging.Logger;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.event.LoggableEvent;
import java.util.function.Supplier;

final class UserLogger {

  private static final Logger LOGGER = Logging.eventsLogger();
  private final String selfName;

  public UserLogger(ActorRef<?> self) {
    this.selfName = name(self);
  }

  private static String name(ActorRef<?> user) {
    return user.path().name();
  }

  public void logContact(ActorRef<?> contact) {
    logEvent(ContactEvent.class, () -> contactEvent(contact));
  }

  private <T extends LoggableEvent> void logEvent(Class<T> type, Supplier<T> event) {
    LOGGER.log(LoggableEvent.KEY, type, event);
  }

  private ContactEvent contactEvent(ActorRef<?> contact) {
    return ContactEvent.builder().user(selfName).addUsers(selfName, name(contact)).build();
  }

  public void logSendCached(ActorRef<?> contact, RiskScoreMsg cached) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(contact, cached));
  }

  private SendCachedEvent sendCachedEvent(ActorRef<?> contact, RiskScoreMsg cached) {
    return SendCachedEvent.builder()
        .from(name(cached.replyTo()))
        .to(name(contact))
        .score(cached.score())
        .id(cached.id())
        .build();
  }

  public void logSendCurrent(ActorRef<?> contact, RiskScoreMsg current) {
    logEvent(SendCurrentEvent.class, () -> sendCurrentEvent(contact, current));
  }

  private SendCurrentEvent sendCurrentEvent(ActorRef<?> contact, RiskScoreMsg current) {
    return SendCurrentEvent.builder()
        .from(name(current.replyTo()))
        .to(name(contact))
        .score(current.score())
        .id(current.id())
        .build();
  }

  public void logReceive(RiskScoreMsg received) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(received));
  }

  private ReceiveEvent receiveEvent(RiskScoreMsg received) {
    return ReceiveEvent.builder()
        .from(name(received.replyTo()))
        .to(selfName)
        .score(received.score())
        .id(received.id())
        .build();
  }

  public void logUpdate(RiskScoreMsg previous, RiskScoreMsg current) {
    logEvent(UpdateEvent.class, () -> updateEvent(previous, current));
  }

  private UpdateEvent updateEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    return UpdateEvent.builder()
        .from(name(current.replyTo()))
        .to(selfName)
        .oldScore(previous.score())
        .newScore(current.score())
        .oldId(previous.id())
        .newId(current.id())
        .build();
  }

  public void logPropagate(ActorRef<?> contact, RiskScoreMsg propagated) {
    logEvent(PropagateEvent.class, () -> propagateEvent(contact, propagated));
  }

  private PropagateEvent propagateEvent(ActorRef<?> contact, RiskScoreMsg propagated) {
    return PropagateEvent.builder()
        .from(name(propagated.replyTo()))
        .to(name(contact))
        .score(propagated.score())
        .id(propagated.id())
        .build();
  }

  public void logContactsRefresh(int numRemaining, int numExpired) {
    logEvent(ContactsRefreshEvent.class, () -> contactsRefreshEvent(numRemaining, numExpired));
  }

  private ContactsRefreshEvent contactsRefreshEvent(int numRemaining, int numExpired) {
    return ContactsRefreshEvent.builder()
        .user(selfName)
        .numRemaining(numRemaining)
        .numExpired(numExpired)
        .build();
  }

  public void logCurrentRefresh(RiskScoreMsg previous, RiskScoreMsg current) {
    logEvent(CurrentRefreshEvent.class, () -> currentRefreshEvent(previous, current));
  }

  private CurrentRefreshEvent currentRefreshEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    return CurrentRefreshEvent.builder()
        .user(selfName)
        .oldScore(previous.score())
        .newScore(current.score())
        .oldId(previous.id())
        .newId(current.id())
        .build();
  }

  public void logTimeout() {
    logEvent(TimeoutEvent.class, this::timeoutEvent);
  }

  private TimeoutEvent timeoutEvent() {
    return TimeoutEvent.builder().user(selfName).build();
  }

  public void logResume() {
    logEvent(ResumeEvent.class, this::resumeEvent);
  }

  private ResumeEvent resumeEvent() {
    return ResumeEvent.builder().user(selfName).build();
  }
}
