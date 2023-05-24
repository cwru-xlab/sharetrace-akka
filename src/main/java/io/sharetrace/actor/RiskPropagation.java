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
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.AlgorithmMessage;
import io.sharetrace.model.message.ContactMessage;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.model.message.RunMessage;
import io.sharetrace.model.message.TimedOutMessage;
import io.sharetrace.model.message.UserMessage;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.cache.CacheParameters;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.TypedLogger;
import io.sharetrace.util.logging.metric.CreateUsersRuntime;
import io.sharetrace.util.logging.metric.LoggableMetric;
import io.sharetrace.util.logging.metric.MessagePassingRuntime;
import io.sharetrace.util.logging.metric.RiskPropagationRuntime;
import io.sharetrace.util.logging.metric.SendContactsRuntime;
import io.sharetrace.util.logging.metric.SendRiskScoresRuntime;
import java.time.Clock;
import java.time.Duration;
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
 *   <li>Send each {@link UserActor} their symptom score in a {@link RiskScoreMessage}.
 *   <li>For each {@link Contact} in the {@link ContactNetwork}, send a {@link ContactMsg} to each
 *       {@link UserActor} that contains the {@link ActorRef} of the other {@link UserActor}.
 *   <li>Terminate once all {@link UserActor}s have stopped passing messages.
 * </ol>
 *
 * <p>In practice, this implementation is distributed or decentralized. The usage of the {@link
 * ContactNetwork} is only for proof-of-concept and experimentation purposes.
 *
 * @see UserActor
 * @see UserParameters
 * @see CacheParameters
 * @see ContactNetwork
 * @see Contact
 * @see RiskScoreMessage
 * @see ContactMsg
 */
public final class RiskPropagation extends AbstractBehavior<AlgorithmMessage> {

  private static final TypedLogger<LoggableMetric> LOGGER = Logging.metricsLogger();
  private static final String NAME = RiskPropagation.class.getSimpleName();
  private static final Props PROPS =
      DispatcherSelector.fromConfig("sharetrace.risk-propagation.dispatcher");
  private static final Props USER_PROPS =
      DispatcherSelector.fromConfig("sharetrace.user.dispatcher");

  private final UserParameters userParameters;
  private final Dataset dataset;
  private final int numUsers;
  private final Clock clock;
  private final CacheFactory<RiskScoreMessage> cacheFactory;
  private final Timer<Class<? extends LoggableMetric>> timer;
  private final BitSet stopped;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> ctx,
      Dataset dataset,
      UserParameters userParameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory) {
    super(ctx);
    this.dataset = dataset;
    this.numUsers = dataset.contactNetwork().users().size();
    this.userParameters = userParameters;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.timer = new Timer<>();
    this.stopped = new BitSet(numUsers);
  }

  @Builder.Factory
  static Algorithm riskPropagation(
      Dataset dataset,
      UserParameters userParameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory) {
    return Algorithm.builder()
        .name(NAME)
        .props(PROPS)
        .behavior(
            Behaviors.setup(
                ctx ->
                    Behaviors.withMdc(
                        AlgorithmMessage.class,
                        Logging.getMdc(),
                        new RiskPropagation(ctx, dataset, userParameters, clock, cacheFactory))))
        .build();
  }

  private static <T extends LoggableMetric> void logMetric(Class<T> type, Supplier<T> metric) {
    LOGGER.log(LoggableMetric.KEY, type, metric);
  }

  @Override
  public Receive<AlgorithmMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMessage.class, this::handle)
        .onMessage(TimedOutMessage.class, this::handle)
        .build();
  }

  private Behavior<AlgorithmMessage> handle(RunMessage msg) {
    Behavior<AlgorithmMessage> behavior = this;
    if (numUsers > 0) {
      timer.start();
      Map<Integer, ActorRef<UserMessage>> users =
          timer.time(this::newUsers, CreateUsersRuntime.class);
      timer.time(() -> sendSymptomScores(users), SendRiskScoresRuntime.class);
      timer.time(() -> sendContacts(users), SendContactsRuntime.class);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Behavior<AlgorithmMessage> handle(TimedOutMessage msg) {
    Behavior<AlgorithmMessage> behavior = this;
    // Assumes at-least-once message delivery from the user actors.
    stopped.set(msg.user());
    if (stopped.cardinality() == numUsers) {
      timer.stop();
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<Integer, ActorRef<UserMessage>> newUsers() {
    Map<Integer, ActorRef<UserMessage>> users = Collecting.newIntKeyedHashMap(numUsers);
    for (int name : dataset.contactNetwork().users()) {
      // Timeout IDs must be 0-based contiguous to use with 'stopped' BitSet.
      ActorRef<UserMessage> user =
          getContext().spawn(newUser(name), String.valueOf(name), USER_PROPS);
      getContext().watch(user);
      users.put(name, user);
    }
    return users;
  }

  private void sendSymptomScores(Map<Integer, ActorRef<UserMessage>> users) {
    // Assumes at-least-once message delivery to the user actors.
    for (Map.Entry<Integer, ActorRef<UserMessage>> entry : users.entrySet()) {
      int name = entry.getKey();
      ActorRef<UserMessage> user = entry.getValue();
      RiskScore symptomScore = dataset.scoreFactory().get(name);
      user.tell(RiskScoreMessage.of(symptomScore, user));
    }
  }

  private void sendContacts(Map<Integer, ActorRef<UserMessage>> users) {
    // Assumes at-least-once message delivery to the user actors.
    for (Contact contact : dataset.contactNetwork().contacts()) {
      ActorRef<UserMessage> user1 = users.get(contact.user1());
      ActorRef<UserMessage> user2 = users.get(contact.user2());
      user1.tell(ContactMessage.of(user2, contact.time()));
      user2.tell(ContactMessage.of(user1, contact.time()));
    }
  }

  private void logMetrics() {
    logMetric(CreateUsersRuntime.class, this::createUsersRuntime);
    logMetric(SendRiskScoresRuntime.class, this::sendRiskScoresRuntime);
    logMetric(SendContactsRuntime.class, this::sendContactsRuntime);
    logMetric(RiskPropagationRuntime.class, this::riskPropagationRuntime);
    logMetric(MessagePassingRuntime.class, this::messagePassingRuntime);
  }

  private Behavior<UserMessage> newUser(int timeoutId) {
    return UserBuilder.create()
        .riskPropagation(getContext().getSelf())
        .timeoutId(timeoutId)
        .userParameters(userParameters)
        .clock(clock)
        .cache(cacheFactory.newCache())
        .build();
  }

  private CreateUsersRuntime createUsersRuntime() {
    return CreateUsersRuntime.of(timer.duration(CreateUsersRuntime.class));
  }

  private SendRiskScoresRuntime sendRiskScoresRuntime() {
    return SendRiskScoresRuntime.of(timer.duration(SendRiskScoresRuntime.class));
  }

  private SendContactsRuntime sendContactsRuntime() {
    return SendContactsRuntime.of(timer.duration(SendContactsRuntime.class));
  }

  private RiskPropagationRuntime riskPropagationRuntime() {
    return RiskPropagationRuntime.of(timer.duration(RiskPropagationRuntime.class));
  }

  private MessagePassingRuntime messagePassingRuntime() {
    Duration total = timer.duration(RiskPropagationRuntime.class);
    Duration sendRiskScoresRuntime = timer.duration(SendRiskScoresRuntime.class);
    Duration createUsersRuntime = timer.duration(CreateUsersRuntime.class);
    Duration exclude = sendRiskScoresRuntime.plus(createUsersRuntime);
    return MessagePassingRuntime.of(total.minus(exclude));
  }
}
