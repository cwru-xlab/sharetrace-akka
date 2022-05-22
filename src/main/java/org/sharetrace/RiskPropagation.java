package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.builder.Builder;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.ScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.ContactMessage;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.RunMessage;
import org.sharetrace.message.UserMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.TypedSupplier;

/**
 * A non-iterative, asynchronous implementation of the ShareTrace algorithm. The objective is to
 * estimate the marginal posterior probability of infection for all individuals in the specified
 * {@link ContactNetwork}. The main steps of the algorithm are as follows:
 *
 * <ol>
 *   <li>Map the {@link ContactNetwork} to a collection {@link User} actors.
 *   <li>For each {@link User}, send it an initial {@link RiskScoreMessage}.
 *   <li>For each pair of {@link User}s that correspond to an edge in the {@link ContactNetwork},
 *       send each a complimentary {@link ContactMessage} that contains the {@link ActorRef} and
 *       time of contact of the other {@link User}.
 *   <li>Terminate once the stopping condition is satisfied. Termination occurs when when all {@link
 *       User}s have stopped passing messages (default), or a certain amount of time has passed.
 * </ol>
 *
 * @see UserParameters
 */
public class RiskPropagation extends AbstractBehavior<AlgorithmMessage> {

  private final Loggables loggables;
  private final UserParameters parameters;
  private final ContactNetwork contactNetwork;
  private final long nUsers;
  private final Clock clock;
  private final ScoreFactory scoreFactory;
  private final ContactTimeFactory contactTimeFactory;
  private final CacheFactory<RiskScoreMessage> cacheFactory;
  private Instant startedAt;
  private int nStopped;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> context,
      Set<Class<? extends Loggable>> loggable,
      ContactNetwork contactNetwork,
      UserParameters parameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory) {
    super(context);
    this.loggables = Loggables.create(loggable, () -> getContext().getLog());
    this.contactNetwork = contactNetwork;
    this.parameters = parameters;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.contactTimeFactory = contactTimeFactory;
    this.nUsers = contactNetwork.nUsers();
    this.nStopped = 0;
  }

  @Builder.Factory
  static Behavior<AlgorithmMessage> riskPropagation(
      ContactNetwork contactNetwork,
      Set<Class<? extends Loggable>> loggable,
      UserParameters parameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory) {
    return Behaviors.setup(
        context -> {
          context.setLoggerName(Logging.METRIC_LOGGER_NAME);
          return new RiskPropagation(
              context,
              loggable,
              contactNetwork,
              parameters,
              clock,
              cacheFactory,
              scoreFactory,
              contactTimeFactory);
        });
  }

  @Override
  public Receive<AlgorithmMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMessage.class, this::onRun)
        .onSignal(Terminated.class, this::onTerminate)
        .build();
  }

  private Behavior<AlgorithmMessage> onRun(RunMessage run) {
    Behavior<AlgorithmMessage> behavior = this;
    if (nUsers > 0) {
      Map<Integer, ActorRef<UserMessage>> users = newUsers();
      startedAt = clock.instant();
      setScores(users);
      setContacts(users);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Behavior<AlgorithmMessage> onTerminate(Terminated terminated) {
    Behavior<AlgorithmMessage> behavior = this;
    if (++nStopped == nUsers) {
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<Integer, ActorRef<UserMessage>> newUsers() {
    Map<Integer, ActorRef<UserMessage>> users = new Object2ObjectOpenHashMap<>();
    contactNetwork.users().forEach(name -> users.put(name, newUser(name)));
    return users;
  }

  private void setScores(Map<Integer, ActorRef<UserMessage>> users) {
    users.forEach(this::sendFirstScore);
  }

  private void setContacts(Map<?, ActorRef<UserMessage>> users) {
    contactNetwork.contacts().forEach(contact -> sendContact(contact, users));
  }

  private void logMetrics() {
    loggables.info(LoggableMetric.KEY, runtimeMetric());
  }

  private ActorRef<UserMessage> newUser(int name) {
    ActorRef<UserMessage> user = getContext().spawn(newUser(), String.valueOf(name));
    getContext().watch(user);
    return user;
  }

  private void sendFirstScore(int name, ActorRef<UserMessage> user) {
    user.tell(RiskScoreMessage.builder().score(scoreFactory.getScore(name)).replyTo(user).build());
  }

  private void sendContact(List<Integer> contact, Map<?, ActorRef<UserMessage>> users) {
    ActorRef<UserMessage> user1 = users.get(contact.get(0));
    ActorRef<UserMessage> user2 = users.get(contact.get(1));
    Instant timestamp = contactTimeFactory.getContactTime(contact.get(0), contact.get(1));
    user1.tell(ContactMessage.builder().replyTo(user2).timestamp(timestamp).build());
    user2.tell(ContactMessage.builder().replyTo(user1).timestamp(timestamp).build());
  }

  private TypedSupplier<LoggableMetric> runtimeMetric() {
    return TypedSupplier.of(RuntimeMetric.class, () -> RuntimeMetric.of(runtime()));
  }

  private Behavior<UserMessage> newUser() {
    return Behaviors.withTimers(
        timers ->
            UserBuilder.create()
                .timers(timers)
                .addAllLoggable(loggables.loggable())
                .parameters(parameters)
                .clock(clock)
                .cache(cacheFactory.create())
                .build());
  }

  private double runtime() {
    return Duration.between(startedAt, clock.instant()).toNanos() / 1e9;
  }
}
