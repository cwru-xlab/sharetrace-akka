package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.data.factory.CacheFactory;
import io.sharetrace.graph.Contact;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.UserParams;
import io.sharetrace.model.message.AlgorithmMsg;
import io.sharetrace.model.message.ContactMsg;
import io.sharetrace.model.message.RiskScoreMsg;
import io.sharetrace.model.message.RunMsg;
import io.sharetrace.model.message.TimedOutMsg;
import io.sharetrace.model.message.UserMsg;
import io.sharetrace.util.CacheParams;
import io.sharetrace.util.logging.Logger;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.metric.CreateUsersRuntime;
import io.sharetrace.util.logging.metric.LoggableMetric;
import io.sharetrace.util.logging.metric.MsgPassingRuntime;
import io.sharetrace.util.logging.metric.RiskPropRuntime;
import io.sharetrace.util.logging.metric.SendContactsRuntime;
import io.sharetrace.util.logging.metric.SendScoresRuntime;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.time.Clock;
import java.util.BitSet;
import java.util.Map;
import java.util.function.Supplier;
import org.immutables.builder.Builder;

/**
 * A non-iterative, asynchronous, concurrent implementation of the ShareTrace algorithm. The
 * objective is to estimate the marginal posterior infection probability (MPIP) for all users. The
 * main steps of the algorithm are as follows:
 *
 * <ol>
 *   <li>For each vertex in the {@link ContactNetwork}, create a {@link UserActor} actor.
 *   <li>Send each {@link UserActor} their symptom score in a {@link RiskScoreMsg}.
 *   <li>For each {@link Contact} in the {@link ContactNetwork}, send a {@link ContactMsg} to each
 *       {@link UserActor} that contains the {@link ActorRef} of the other {@link UserActor}.
 *   <li>Terminate once all {@link UserActor}s have stopped passing messages.
 * </ol>
 *
 * In practice, this implementation is distributed or decentralized. The usage of the {@link
 * ContactNetwork} is only for proof-of-concept and experimentation purposes.
 *
 * @see UserActor
 * @see UserParams
 * @see CacheParams
 * @see ContactNetwork
 * @see Contact
 * @see RiskScoreMsg
 * @see ContactMsg
 */
public final class RiskPropagation extends AbstractBehavior<AlgorithmMsg> {

  private static final Logger LOGGER = Logging.metricsLogger();
  private static final String NAME = RiskPropagation.class.getSimpleName();
  private static final Props PROPS = DispatcherSelector.fromConfig("algorithm-dispatcher");
  private static final Props USER_PROPS = DispatcherSelector.fromConfig("user-dispatcher");

  private final UserParams userParams;
  private final Dataset dataset;
  private final int numUsers;
  private final Clock clock;
  private final CacheFactory<RiskScoreMsg> cacheFactory;
  private final Timer<Class<? extends LoggableMetric>> timer;
  private final BitSet stopped;

  private RiskPropagation(
      ActorContext<AlgorithmMsg> ctx,
      Dataset dataset,
      UserParams userParams,
      Clock clock,
      CacheFactory<RiskScoreMsg> cacheFactory) {
    super(ctx);
    this.dataset = dataset;
    this.numUsers = dataset.contactNetwork().users().size();
    this.userParams = userParams;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.timer = new Timer<>();
    this.stopped = new BitSet(numUsers);
  }

  @Builder.Factory
  static Algorithm riskPropagation(
      Dataset dataset,
      UserParams userParams,
      Clock clock,
      CacheFactory<RiskScoreMsg> cacheFactory) {
    Behavior<AlgorithmMsg> behavior =
        Behaviors.setup(ctx -> new RiskPropagation(ctx, dataset, userParams, clock, cacheFactory));
    behavior = Behaviors.withMdc(AlgorithmMsg.class, Logging.mdc(), behavior);
    return Algorithm.of(behavior, NAME, PROPS);
  }

  @Override
  public Receive<AlgorithmMsg> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMsg.class, this::handle)
        .onMessage(TimedOutMsg.class, this::handle)
        .build();
  }

  private Behavior<AlgorithmMsg> handle(RunMsg msg) {
    Behavior<AlgorithmMsg> behavior = this;
    if (numUsers > 0) {
      timer.start();
      Map<Integer, ActorRef<UserMsg>> users = timer.time(this::newUsers, CreateUsersRuntime.class);
      timer.time(() -> sendSymptomScores(users), SendScoresRuntime.class);
      timer.time(() -> sendContacts(users), SendContactsRuntime.class);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Behavior<AlgorithmMsg> handle(TimedOutMsg msg) {
    Behavior<AlgorithmMsg> behavior = this;
    // Assumes at-least-once message delivery from the user actors.
    stopped.set(msg.user());
    if (stopped.cardinality() == numUsers) {
      timer.stop();
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<Integer, ActorRef<UserMsg>> newUsers() {
    Map<Integer, ActorRef<UserMsg>> users = new Int2ObjectOpenHashMap<>(numUsers);
    for (int name : dataset.contactNetwork().users()) {
      // Timeout IDs must be 0-based contiguous to use with 'stopped' BitSet.
      ActorRef<UserMsg> user = getContext().spawn(newUser(name), String.valueOf(name), USER_PROPS);
      getContext().watch(user);
      users.put(name, user);
    }
    return users;
  }

  private void sendSymptomScores(Map<Integer, ActorRef<UserMsg>> users) {
    // Assumes at-least-once message delivery to the user actors.
    for (Map.Entry<Integer, ActorRef<UserMsg>> entry : users.entrySet()) {
      int name = entry.getKey();
      ActorRef<UserMsg> user = entry.getValue();
      RiskScore symptomScore = dataset.scoreFactory().riskScore(name);
      user.tell(RiskScoreMsg.of(symptomScore, user));
    }
  }

  private void sendContacts(Map<Integer, ActorRef<UserMsg>> users) {
    // Assumes at-least-once message delivery to the user actors.
    for (Contact contact : dataset.contactNetwork().contacts()) {
      ActorRef<UserMsg> user1 = users.get(contact.user1());
      ActorRef<UserMsg> user2 = users.get(contact.user2());
      user1.tell(ContactMsg.of(user2, contact.time()));
      user2.tell(ContactMsg.of(user1, contact.time()));
    }
  }

  private void logMetrics() {
    logMetric(CreateUsersRuntime.class, this::createRuntime);
    logMetric(SendScoresRuntime.class, this::scoresRuntime);
    logMetric(SendContactsRuntime.class, this::contactsRuntime);
    logMetric(RiskPropRuntime.class, this::riskPropRuntime);
    logMetric(MsgPassingRuntime.class, this::msgPassingRuntime);
  }

  private Behavior<UserMsg> newUser(int timeoutId) {
    return UserBuilder.create()
        .riskProp(getContext().getSelf())
        .timeoutId(timeoutId)
        .userParams(userParams)
        .clock(clock)
        .cache(cacheFactory.newCache())
        .build();
  }

  private <T extends LoggableMetric> void logMetric(Class<T> type, Supplier<T> metric) {
    LOGGER.log(LoggableMetric.KEY, type, metric);
  }

  private CreateUsersRuntime createRuntime() {
    return CreateUsersRuntime.of(timer.millis(CreateUsersRuntime.class));
  }

  private SendScoresRuntime scoresRuntime() {
    return SendScoresRuntime.of(timer.millis(SendScoresRuntime.class));
  }

  private SendContactsRuntime contactsRuntime() {
    return SendContactsRuntime.of(timer.millis(SendContactsRuntime.class));
  }

  private RiskPropRuntime riskPropRuntime() {
    return RiskPropRuntime.of(timer.millis(RiskPropRuntime.class));
  }

  private MsgPassingRuntime msgPassingRuntime() {
    long total = timer.nanos(RiskPropRuntime.class);
    long exclude = timer.nanos(SendScoresRuntime.class) + timer.nanos(CreateUsersRuntime.class);
    return MsgPassingRuntime.of(Timer.millis(total - exclude));
  }
}
