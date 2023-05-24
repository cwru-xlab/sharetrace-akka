package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.AlgorithmMessage;
import io.sharetrace.model.message.ContactMessage;
import io.sharetrace.model.message.ContactsRefreshMessage;
import io.sharetrace.model.message.CurrentRefreshMessage;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.model.message.ThresholdMessage;
import io.sharetrace.model.message.TimedOutMessage;
import io.sharetrace.model.message.UserMessage;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.cache.IntervalCache;
import io.sharetrace.util.logging.Logging;
import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Predicate;
import org.immutables.builder.Builder;

public final class UserActor extends AbstractBehavior<UserMessage> {

  private final ActorRef<AlgorithmMessage> riskPropagation;
  private final TimedOutMessage timedOutMessage;
  private final TimerScheduler<UserMessage> timers;
  private final UserLogger logger;
  private final UserParameters userParameters;
  private final Clock clock;
  private final IntervalCache<RiskScoreMessage> cache;
  private final Map<ActorRef<?>, ContactActor> contacts;
  private final MsgUtil messageUtil;
  private final RiskScoreMessage defaultCurrent;
  private RiskScoreMessage current;
  private RiskScoreMessage transmitted;

  private UserActor(
      ActorContext<UserMessage> context,
      ActorRef<AlgorithmMessage> riskPropagation,
      int timeoutId,
      TimerScheduler<UserMessage> timers,
      UserParameters userParameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    super(context);
    this.riskPropagation = riskPropagation;
    this.timedOutMessage = TimedOutMessage.of(timeoutId);
    this.timers = timers;
    this.logger = new UserLogger(getContext().getSelf(), clock);
    this.userParameters = userParameters;
    this.clock = clock;
    this.cache = cache;
    this.contacts = Collecting.newHashMap();
    this.messageUtil = new MsgUtil(getContext().getSelf(), clock, userParameters);
    this.defaultCurrent = messageUtil.defaultMessage();
  }

  @Builder.Factory
  static Behavior<UserMessage> user(
      ActorRef<AlgorithmMessage> riskPropagation,
      int timeoutId,
      UserParameters userParameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    return Behaviors.setup(
        context -> {
          Behavior<UserMessage> user =
              Behaviors.withTimers(
                  timers ->
                      new UserActor(
                          context,
                          riskPropagation,
                          timeoutId,
                          timers,
                          userParameters,
                          clock,
                          cache));
          return Behaviors.withMdc(UserMessage.class, Logging.getMdc(), user);
        });
  }

  @Override
  public Receive<UserMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMessage.class, this::handle)
        .onMessage(RiskScoreMessage.class, this::handle)
        .onMessage(CurrentRefreshMessage.class, this::handle)
        .onMessage(ContactsRefreshMessage.class, this::handle)
        .onMessage(ThresholdMessage.class, this::handle)
        .onMessage(TimedOutMessage.class, this::handle)
        .build();
  }

  private Behavior<UserMessage> handle(ContactMessage message) {
    if (messageUtil.isContactAlive(message)) {
      ContactActor contact = addNewContact(message);
      startContactsRefreshTimer();
      logger.logContactEvent(contact.ref());
      sendCurrentOrCached(contact);
    }
    return this;
  }

  private Behavior<UserMessage> handle(RiskScoreMessage message) {
    logger.logReceiveEvent(message);
    cache.put(message.timestamp(), message);
    RiskScoreMessage transmit = updateIfAboveCurrent(message);
    propagate(transmit);
    resetTimeout();
    return this;
  }

  @SuppressWarnings("unused")
  private Behavior<UserMessage> handle(CurrentRefreshMessage message) {
    RiskScoreMessage cachedOrDefault = cache.max(clock.instant()).orElse(defaultCurrent);
    RiskScoreMessage previous = updateCurrent(cachedOrDefault);
    logger.logCurrentRefreshEvent(previous, current);
    if (current != defaultCurrent) {
      startCurrentRefreshTimer();
    }
    return this;
  }

  @SuppressWarnings("unused")
  private Behavior<UserMessage> handle(ContactsRefreshMessage message) {
    int current = contacts.size();
    contacts.values().removeIf(Predicate.not(ContactActor::isAlive));
    int remaining = contacts.size();
    int expired = current - remaining;
    logger.logContactsRefreshEvent(remaining, expired);
    startContactsRefreshTimer();
    return this;
  }

  private Behavior<UserMessage> handle(ThresholdMessage message) {
    /* There may be a delay between when the contact actor sets the timer and when this actor
    processes the message. It is possible that this actor refreshes its contacts, removing the
    contact that set the threshold timer. So we need to check that the contact still exists. */
    ContactActor contact = contacts.get(message.contact());
    boolean hasNotExpired = contact != null;
    if (hasNotExpired) {
      contact.updateThreshold();
    }
    return this;
  }

  private Behavior<UserMessage> handle(TimedOutMessage message) {
    riskPropagation.tell(message);
    return this;
  }

  private ContactActor addNewContact(ContactMessage message) {
    ContactActor contact = new ContactActor(message, timers, messageUtil, cache);
    contacts.put(contact.ref(), contact);
    return contact;
  }

  private void startContactsRefreshTimer() {
    /* There may be a delay between when this timer expires and when the user actor processes the
    message. While this timer is based on the minimum contact TTL, the delay to refresh contacts
    may be such that all contacts expire. Thus, a new refresh timer may not always be started. */
    contacts.values().stream()
        .min(Comparator.naturalOrder())
        .map(ContactActor::timeToLive)
        .ifPresent(
            minTimeToLive ->
                timers.startSingleTimer(ContactsRefreshMessage.INSTANCE, minTimeToLive));
  }

  private void sendCurrentOrCached(ContactActor contact) {
    if (contact.shouldReceive(current)) {
      sendCurrent(contact);
    } else {
      sendCached(contact);
    }
  }

  private RiskScoreMessage updateIfAboveCurrent(RiskScoreMessage message) {
    RiskScoreMessage transmitted;
    if (!isInitialized() || message.value() > current.value()) {
      RiskScoreMessage previous = updateCurrent(message);
      logger.logUpdateEvent(previous, current);
      transmitted = this.transmitted;
      if (previous != defaultCurrent) {
        startCurrentRefreshTimer();
      }
    } else {
      transmitted = messageUtil.transmitted(message);
    }
    return transmitted;
  }

  private void propagate(RiskScoreMessage message) {
    contacts.values().stream()
        .filter(contact -> contact.shouldReceive(message))
        .forEach(contact -> contact.tell(message, logger::logPropagateEvent));
  }

  private void resetTimeout() {
    timers.startSingleTimer(timedOutMessage, userParameters.idleTimeout());
  }

  private void startCurrentRefreshTimer() {
    timers.startSingleTimer(CurrentRefreshMessage.INSTANCE, messageUtil.scoreTimeToLive(current));
  }

  private void sendCurrent(ContactActor contact) {
    contact.tell(transmitted, logger::logSendCurrentEvent);
  }

  private void sendCached(ContactActor contact) {
    cache
        .max(contact.bufferedContactTime())
        .filter(messageUtil::isScoreAlive)
        .map(messageUtil::transmitted)
        .ifPresent(cached -> contact.tell(cached, logger::logSendCachedEvent));
  }

  private RiskScoreMessage updateCurrent(RiskScoreMessage message) {
    RiskScoreMessage previous = isInitialized() ? current : defaultCurrent;
    current = message;
    transmitted = messageUtil.transmitted(current);
    return previous;
  }

  /* This is a hack to ensure the symptom score is always logged for analysis. This covers the
  edge case where the symptom score has a value of 0, which would not otherwise be logged. */
  private boolean isInitialized() {
    return current != null;
  }
}
