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
import io.sharetrace.logging.event.SendCachedEvent;
import io.sharetrace.logging.event.SendCurrentEvent;
import io.sharetrace.logging.event.TimeoutEvent;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.util.TypedSupplier;
import java.util.Set;
import java.util.function.Supplier;

final class UserLogger {

  private final Logger logger;
  private final String userName;

  public UserLogger(Set<Class<? extends Loggable>> loggable, ActorContext<?> ctx) {
    this.logger = Logging.logger(loggable, ctx::getLog);
    this.userName = name(ctx.getSelf());
  }

  private static String name(ActorRef<?> user) {
    return user.path().name();
  }

  public void logContact(ActorRef<?> contact) {
    log(ContactEvent.class, () -> contactEvent(contact));
  }

  public void logSendCached(ActorRef<?> contact, RiskScoreMsg cached) {
    log(SendCachedEvent.class, () -> sendCachedEvent(contact, cached));
  }

  public void logSendCurrent(ActorRef<?> contact, RiskScoreMsg current) {
    log(SendCurrentEvent.class, () -> sendCurrentEvent(contact, current));
  }

  public void logReceive(RiskScoreMsg received) {
    log(ReceiveEvent.class, () -> receiveEvent(received));
  }

  public void logUpdate(RiskScoreMsg previous, RiskScoreMsg current) {
    log(UpdateEvent.class, () -> updateEvent(previous, current));
  }

  public void logPropagate(ActorRef<?> contact, RiskScoreMsg propagated) {
    log(PropagateEvent.class, () -> propagateEvent(contact, propagated));
  }

  public void logContactsRefresh(int numRemaining, int numExpired) {
    log(ContactsRefreshEvent.class, () -> contactsRefreshEvent(numRemaining, numExpired));
  }

  public void logCurrentRefresh(RiskScoreMsg previous, RiskScoreMsg current) {
    log(CurrentRefreshEvent.class, () -> currentRefreshEvent(previous, current));
  }

  public void logTimeout() {
    log(TimeoutEvent.class, this::timeoutEvent);
  }

  private <T extends Loggable> void log(Class<T> type, Supplier<T> supplier) {
    logger.log(LoggableEvent.KEY, TypedSupplier.of(type, supplier));
  }

  private ContactEvent contactEvent(ActorRef<?> contact) {
    return ContactEvent.builder().user(userName).addUsers(userName, name(contact)).build();
  }

  private SendCachedEvent sendCachedEvent(ActorRef<?> contact, RiskScoreMsg cached) {
    return SendCachedEvent.builder()
        .from(name(cached.replyTo()))
        .to(name(contact))
        .score(cached.score())
        .id(cached.id())
        .build();
  }

  private SendCurrentEvent sendCurrentEvent(ActorRef<?> contact, RiskScoreMsg current) {
    return SendCurrentEvent.builder()
        .from(name(current.replyTo()))
        .to(name(contact))
        .score(current.score())
        .id(current.id())
        .build();
  }

  private ReceiveEvent receiveEvent(RiskScoreMsg received) {
    return ReceiveEvent.builder()
        .from(name(received.replyTo()))
        .to(userName)
        .score(received.score())
        .id(received.id())
        .build();
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

  private PropagateEvent propagateEvent(ActorRef<?> contact, RiskScoreMsg propagated) {
    return PropagateEvent.builder()
        .from(name(propagated.replyTo()))
        .to(name(contact))
        .score(propagated.score())
        .id(propagated.id())
        .build();
  }

  private ContactsRefreshEvent contactsRefreshEvent(int numRemaining, int numExpired) {
    return ContactsRefreshEvent.builder()
        .user(userName)
        .numRemaining(numRemaining)
        .numExpired(numExpired)
        .build();
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

  private TimeoutEvent timeoutEvent() {
    return TimeoutEvent.builder().user(userName).build();
  }
}
