package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.UserParams;
import io.sharetrace.model.message.*;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.IntervalCache;
import io.sharetrace.util.logging.Logging;
import org.immutables.builder.Builder;

import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * An actor that corresponds to a vertex in a {@link ContactNetwork}. Collectively, all {@link
 * UserActor}s carry out the execution of {@link RiskPropagation}.
 *
 * @see RiskPropagation
 * @see RiskScoreMsg
 * @see ContactMsg
 * @see TimedOutMsg
 * @see ContactsRefreshMsg
 * @see CurrentRefreshMsg
 * @see UserParams
 * @see IntervalCache
 */
public final class UserActor extends AbstractBehavior<UserMsg> {

    private final ActorRef<AlgorithmMsg> riskProp;
    private final TimedOutMsg timedOutMsg;
    private final TimerScheduler<UserMsg> timers;
    private final UserLogger logger;
    private final UserParams userParams;
    private final Clock clock;
    private final IntervalCache<RiskScoreMsg> cache;
    private final Map<ActorRef<?>, ContactActor> contacts;
    private final MsgUtil msgUtil;
    private final RiskScoreMsg defaultCurrent;
    private RiskScoreMsg current;
    private RiskScoreMsg transmitted;

    private UserActor(
            ActorContext<UserMsg> ctx,
            ActorRef<AlgorithmMsg> riskProp,
            int timeoutId,
            TimerScheduler<UserMsg> timers,
            UserParams userParams,
            Clock clock,
            IntervalCache<RiskScoreMsg> cache) {
        super(ctx);
        this.riskProp = riskProp;
        this.timedOutMsg = TimedOutMsg.of(timeoutId);
        this.timers = timers;
        this.logger = new UserLogger(getContext().getSelf());
        this.userParams = userParams;
        this.clock = clock;
        this.cache = cache;
        this.contacts = Collecting.newHashMap();
        this.msgUtil = new MsgUtil(getContext().getSelf(), clock, userParams);
        this.defaultCurrent = msgUtil.defaultMsg();
    }

    @Builder.Factory
    static Behavior<UserMsg> user(
            ActorRef<AlgorithmMsg> riskProp,
            int timeoutId,
            UserParams userParams,
            Clock clock,
            IntervalCache<RiskScoreMsg> cache) {
        return Behaviors.setup(
                ctx -> {
                    Behavior<UserMsg> user =
                            Behaviors.withTimers(
                                    timers ->
                                            new UserActor(ctx, riskProp, timeoutId, timers, userParams, clock, cache));
                    return Behaviors.withMdc(UserMsg.class, Logging.getMdc(), user);
                });
    }

    @Override
    public Receive<UserMsg> createReceive() {
        return newReceiveBuilder()
                .onMessage(ContactMsg.class, this::handle)
                .onMessage(RiskScoreMsg.class, this::handle)
                .onMessage(CurrentRefreshMsg.class, this::handle)
                .onMessage(ContactsRefreshMsg.class, this::handle)
                .onMessage(ThresholdMsg.class, this::handle)
                .onMessage(TimedOutMsg.class, this::handle)
                .build();
    }

    private Behavior<UserMsg> handle(ContactMsg msg) {
        if (msgUtil.isContactAlive(msg)) {
            ContactActor contact = addNewContact(msg);
            startContactsRefreshTimer();
            logger.logContact(contact.ref());
            sendCurrentOrCached(contact);
        }
        return this;
    }

    private Behavior<UserMsg> handle(RiskScoreMsg msg) {
        logger.logReceive(msg);
        cache.put(msg.score().time(), msg);
        RiskScoreMsg transmit = updateIfAboveCurrent(msg);
        propagate(transmit);
        resetTimeout();
        return this;
    }

    @SuppressWarnings("unused")
    private Behavior<UserMsg> handle(CurrentRefreshMsg msg) {
        RiskScoreMsg cachedOrDefault = cache.max(clock.instant()).orElse(defaultCurrent);
        RiskScoreMsg previous = updateCurrent(cachedOrDefault);
        logger.logCurrentRefresh(previous, current);
        if (current != defaultCurrent) {
            startCurrentRefreshTimer();
        }
        return this;
    }

    @SuppressWarnings("unused")
    private Behavior<UserMsg> handle(ContactsRefreshMsg msg) {
        int numContacts = contacts.size();
        contacts.values().removeIf(Predicate.not(ContactActor::isAlive));
        int numRemaining = contacts.size();
        int numExpired = numContacts - numRemaining;
        logger.logContactsRefresh(numRemaining, numExpired);
        startContactsRefreshTimer();
        return this;
    }

    private Behavior<UserMsg> handle(ThresholdMsg msg) {
    /* There may be a delay between when the contact actor sets the timer and when this actor
    processes the message. It is possible that this actor refreshes its contacts, removing the
    contact that set the threshold timer. So we need to check that the contact still exists. */
        ContactActor contact = contacts.get(msg.contact());
        boolean hasNotExpired = contact != null;
        if (hasNotExpired) {
            contact.updateThreshold();
        }
        return this;
    }

    private Behavior<UserMsg> handle(TimedOutMsg msg) {
        riskProp.tell(msg);
        return this;
    }

    private ContactActor addNewContact(ContactMsg msg) {
        ContactActor contact = new ContactActor(msg, timers, msgUtil, cache);
        contacts.put(contact.ref(), contact);
        return contact;
    }

    private void startContactsRefreshTimer() {
    /* There may be a delay between when this timer expires and when the user actor processes the
    message. While this timer is based on the minimum contact TTL, the delay to refresh contacts
    may be such that all contacts expire. Thus, a new refresh timer may not always be started. */
        contacts.values().stream()
                .min(Comparator.naturalOrder())
                .map(ContactActor::ttl)
                .ifPresent(minTtl -> timers.startSingleTimer(ContactsRefreshMsg.INSTANCE, minTtl));
    }

    private void sendCurrentOrCached(ContactActor contact) {
        if (contact.shouldReceive(current)) {
            sendCurrent(contact);
        } else {
            sendCached(contact);
        }
    }

    private RiskScoreMsg updateIfAboveCurrent(RiskScoreMsg msg) {
        RiskScoreMsg transmit;
        if (!isInitialized() || msgUtil.isGreaterThan(msg, current)) {
            RiskScoreMsg previous = updateCurrent(msg);
            logger.logUpdate(previous, current);
            transmit = transmitted;
            if (previous != defaultCurrent) {
                startCurrentRefreshTimer();
            }
        } else {
            transmit = msgUtil.transmitted(msg);
        }
        return transmit;
    }

    private void propagate(RiskScoreMsg msg) {
        contacts.values().stream()
                .filter(contact -> contact.shouldReceive(msg))
                .forEach(contact -> contact.tell(msg, logger::logPropagate));
    }

    private void resetTimeout() {
        timers.startSingleTimer(timedOutMsg, userParams.idleTimeout());
    }

    private void startCurrentRefreshTimer() {
        timers.startSingleTimer(CurrentRefreshMsg.INSTANCE, msgUtil.computeScoreTtl(current));
    }

    private void sendCurrent(ContactActor contact) {
        contact.tell(transmitted, logger::logSendCurrent);
    }

    private void sendCached(ContactActor contact) {
        cache
                .max(contact.bufferedContactTime())
                .filter(msgUtil::isScoreAlive)
                .map(msgUtil::transmitted)
                .ifPresent(cached -> contact.tell(cached, logger::logSendCached));
    }

    private RiskScoreMsg updateCurrent(RiskScoreMsg msg) {
        RiskScoreMsg previous = isInitialized() ? current : defaultCurrent;
        current = msg;
        transmitted = msgUtil.transmitted(current);
        return previous;
    }

    /* This is a hack to ensure the symptom score is always logged for analysis. This covers the
    edge case where the symptom score has a value of 0, which would not otherwise be logged. */
    private boolean isInitialized() {
        return current != null;
    }
}
