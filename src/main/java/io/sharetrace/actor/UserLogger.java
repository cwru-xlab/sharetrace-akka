package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.Logger;
import io.sharetrace.logging.Logging;
import io.sharetrace.logging.event.ContactEvent;
import io.sharetrace.logging.event.ContactsRefreshEvent;
import io.sharetrace.logging.event.CurrentRefreshEvent;
import io.sharetrace.logging.event.LoggableEvent;
import io.sharetrace.logging.event.PropagateEvent;
import io.sharetrace.logging.event.ReceiveEvent;
import io.sharetrace.logging.event.ResumeEvent;
import io.sharetrace.logging.event.SendCachedEvent;
import io.sharetrace.logging.event.SendCurrentEvent;
import io.sharetrace.logging.event.TimeoutEvent;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.util.TypedSupplier;
import java.util.function.Supplier;

final class UserLogger {

  private final Logger logger;
  private final String userName;

  public UserLogger(ActorContext<?> ctx) {
    this.logger = Logging.logger(ctx.getLog());
    this.userName = name(ctx.getSelf());
  }

  private static String name(ActorRef<?> user) {
    return user.path().name();
  }

  public void logContact(ActorRef<?> contact) {
    log(ContactEvent.class, () -> contactEvent(contact));
  }

  private <T extends Loggable> void log(Class<T> type, Supplier<T> supplier) {
    logger.log(LoggableEvent.KEY, TypedSupplier.of(type, supplier));
  }

  private ContactEvent contactEvent(ActorRef<?> contact) {
    return ContactEvent.builder().user(userName).addUsers(userName, name(contact)).build();
  }

  public void logSendCached(ActorRef<?> contact, RiskScoreMsg cached) {
    log(SendCachedEvent.class, () -> sendCachedEvent(contact, cached));
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
    log(SendCurrentEvent.class, () -> sendCurrentEvent(contact, current));
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
    log(ReceiveEvent.class, () -> receiveEvent(received));
  }

  private ReceiveEvent receiveEvent(RiskScoreMsg received) {
    return ReceiveEvent.builder()
        .from(name(received.replyTo()))
        .to(userName)
        .score(received.score())
        .id(received.id())
        .build();
  }

  public void logUpdate(RiskScoreMsg previous, RiskScoreMsg current) {
    log(UpdateEvent.class, () -> updateEvent(previous, current));
  }

  private UpdateEvent updateEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    return UpdateEvent.builder()
        .from(name(current.replyTo()))
        .to(userName)
        .oldScore(previous.score())
        .newScore(current.score())
        .oldId(previous.id())
        .newId(current.id())
        .build();
  }

  public void logPropagate(ActorRef<?> contact, RiskScoreMsg propagated) {
    log(PropagateEvent.class, () -> propagateEvent(contact, propagated));
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
    log(ContactsRefreshEvent.class, () -> contactsRefreshEvent(numRemaining, numExpired));
  }

  private ContactsRefreshEvent contactsRefreshEvent(int numRemaining, int numExpired) {
    return ContactsRefreshEvent.builder()
        .user(userName)
        .numRemaining(numRemaining)
        .numExpired(numExpired)
        .build();
  }

  public void logCurrentRefresh(RiskScoreMsg previous, RiskScoreMsg current) {
    log(CurrentRefreshEvent.class, () -> currentRefreshEvent(previous, current));
  }

  private CurrentRefreshEvent currentRefreshEvent(RiskScoreMsg previous, RiskScoreMsg current) {
    return CurrentRefreshEvent.builder()
        .user(userName)
        .oldScore(previous.score())
        .newScore(current.score())
        .oldId(previous.id())
        .newId(current.id())
        .build();
  }

  public void logTimeout() {
    log(TimeoutEvent.class, this::timeoutEvent);
  }

  private TimeoutEvent timeoutEvent() {
    return TimeoutEvent.builder().user(userName).build();
  }

  public void logResume() {
    log(ResumeEvent.class, this::resumeEvent);
  }

  private ResumeEvent resumeEvent() {
    return ResumeEvent.builder().user(userName).build();
  }
}
