package sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.immutables.builder.Builder;
import sharetrace.model.UserParameters;
import sharetrace.model.message.AlgorithmMessage;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.ContactsRefreshMessage;
import sharetrace.model.message.CurrentRefreshMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.ThresholdMessage;
import sharetrace.model.message.TimedOutMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Collecting;
import sharetrace.util.cache.IntervalCache;
import sharetrace.util.logging.Logging;

final class User extends AbstractBehavior<UserMessage> {

  private final ActorRef<AlgorithmMessage> coordinator;
  private final TimedOutMessage timedOutMessage;
  private final TimerScheduler<UserMessage> timers;
  private final UserLogger logger;
  private final UserParameters userParameters;
  private final Clock clock;
  private final IntervalCache<RiskScoreMessage> cache;
  private final Map<ActorRef<?>, Contact> contacts;
  private final UserHelper helper;
  private final RiskScoreMessage defaultCurrent;

  private RiskScoreMessage current;
  private RiskScoreMessage transmitted;

  private User(
      ActorContext<UserMessage> context,
      ActorRef<AlgorithmMessage> coordinator,
      int timeoutId,
      TimerScheduler<UserMessage> timers,
      UserParameters userParameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    super(context);
    this.coordinator = coordinator;
    this.timers = timers;
    this.userParameters = userParameters;
    this.clock = clock;
    this.cache = cache;
    this.contacts = Collecting.newHashMap();
    this.timedOutMessage = TimedOutMessage.of(timeoutId);
    this.logger = new UserLogger(getContext().getSelf(), clock);
    this.helper = new UserHelper(getContext().getSelf(), clock, userParameters);
    this.defaultCurrent = helper.defaultMessage();
    this.current = defaultCurrent;
    this.transmitted = defaultCurrent;
  }

  @Builder.Factory
  static Behavior<UserMessage> user(
      ActorRef<AlgorithmMessage> coordinator,
      int timeoutId,
      UserParameters userParameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    return Behaviors.setup(
        context -> {
          Behavior<UserMessage> user =
              Behaviors.withTimers(
                  timers ->
                      new User(
                          context, coordinator, timeoutId, timers, userParameters, clock, cache));
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
    if (!helper.isExpired(message)) {
      Contact contact = addContact(message);
      logger.logContactEvent(contact.reference());
      startContactsRefreshTimer();
      sendCurrentOrCached(contact);
    }
    return this;
  }

  private Behavior<UserMessage> handle(RiskScoreMessage message) {
    logger.logReceiveEvent(message);
    cache.put(message.timestamp(), message);
    RiskScoreMessage transmitted = updateIfAboveCurrent(message);
    propagate(transmitted);
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
    contacts.values().removeIf(Contact::isExpired);
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
    Optional.of(message)
        .map(ThresholdMessage::contact)
        .map(contacts::get)
        .ifPresent(Contact::updateThreshold);
    return this;
  }

  private Behavior<UserMessage> handle(TimedOutMessage message) {
    coordinator.tell(message);
    return this;
  }

  private Contact addContact(ContactMessage message) {
    Contact contact = new Contact(message, timers, helper, cache);
    contacts.put(contact.reference(), contact);
    return contact;
  }

  private void startContactsRefreshTimer() {
    /* There may be a delay between when this timer expires and when the user actor processes the
    message. While this timer is based on the minimum contact TTL, the delay to refresh contacts
    may be such that all contacts expire. Thus, a new refresh timer may not always be started. */
    contacts.values().stream()
        .min(Comparator.naturalOrder())
        .map(Contact::untilExpiry)
        .ifPresent(expiry -> timers.startSingleTimer(ContactsRefreshMessage.INSTANCE, expiry));
  }

  private void sendCurrentOrCached(Contact contact) {
    if (contact.shouldReceive(current)) {
      sendCurrent(contact);
    } else {
      sendCached(contact);
    }
  }

  private RiskScoreMessage updateIfAboveCurrent(RiskScoreMessage message) {
    if (!helper.isAbove(message, current)) {
      return helper.transmitted(message);
    }
    RiskScoreMessage previous = updateCurrent(message);
    logger.logUpdateEvent(previous, current);
    if (previous != defaultCurrent) {
      startCurrentRefreshTimer();
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
    timers.startSingleTimer(CurrentRefreshMessage.INSTANCE, helper.untilExpiry(current));
  }

  private void sendCurrent(Contact contact) {
    contact.tell(transmitted, logger::logSendCurrentEvent);
  }

  private void sendCached(Contact contact) {
    cache
        .max(contact.bufferedTimestamp())
        .filter(Predicate.not(helper::isExpired))
        .map(helper::transmitted)
        .ifPresent(cached -> contact.tell(cached, logger::logSendCachedEvent));
  }

  private RiskScoreMessage updateCurrent(RiskScoreMessage message) {
    RiskScoreMessage previous = current;
    current = message;
    transmitted = helper.transmitted(current);
    return previous;
  }
}
