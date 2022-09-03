package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.time.Clock;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.time.StopWatch;
import org.immutables.builder.Builder;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.Contact;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.ContactMessage;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.RunMessage;
import org.sharetrace.message.UserMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.TypedSupplier;
import org.slf4j.MDC;

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
  private final Map<String, String> mdc;
  private final UserParameters parameters;
  private final ContactNetwork contactNetwork;
  private final Clock clock;
  private final RiskScoreFactory riskScoreFactory;
  private final CacheFactory<RiskScoreMessage> cacheFactory;
  private StopWatch runtime;
  private int nStopped;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> context,
      Set<Class<? extends Loggable>> loggable,
      Map<String, String> mdc,
      ContactNetwork contactNetwork,
      UserParameters parameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory,
      RiskScoreFactory riskScoreFactory) {
    super(context);
    this.loggables = Loggables.create(loggable, () -> getContext().getLog());
    this.mdc = mdc;
    this.contactNetwork = contactNetwork;
    this.parameters = parameters;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.riskScoreFactory = riskScoreFactory;
    this.nStopped = 0;
  }

  @Builder.Factory
  static Behavior<AlgorithmMessage> riskPropagation(
      ContactNetwork contactNetwork,
      Set<Class<? extends Loggable>> loggable,
      Map<String, String> mdc,
      UserParameters parameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory,
      RiskScoreFactory riskScoreFactory) {
    return Behaviors.setup(
        context -> {
          context.setLoggerName(Logging.METRIC_LOGGER_NAME);
          return new RiskPropagation(
              context,
              loggable,
              mdc,
              contactNetwork,
              parameters,
              clock,
              cacheFactory,
              riskScoreFactory);
        });
  }

  @Override
  public Receive<AlgorithmMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMessage.class, this::onRunMessage)
        .onSignal(Terminated.class, this::onTerminate)
        .build();
  }

  private Behavior<AlgorithmMessage> onRunMessage(RunMessage message) {
    Behavior<AlgorithmMessage> behavior = this;
    if (contactNetwork.nUsers() > 0) {
      Map<Integer, ActorRef<UserMessage>> users = newUsers();
      sendSymptomScores(users);
      runtime = StopWatch.createStarted();
      sendContacts(users);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Behavior<AlgorithmMessage> onTerminate(Terminated terminated) {
    Behavior<AlgorithmMessage> behavior = this;
    if (++nStopped == contactNetwork.nUsers()) {
      mdc.forEach(MDC::put);
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<Integer, ActorRef<UserMessage>> newUsers() {
    Map<Integer, ActorRef<UserMessage>> users = newUserMap();
    Iterable<Integer> names = () -> contactNetwork.users().iterator();
    ActorRef<UserMessage> user;
    for (int name : names) {
      user = getContext().spawn(newUser(), String.valueOf(name));
      getContext().watch(user);
      users.put(name, user);
    }
    return users;
  }

  private void sendSymptomScores(Map<Integer, ActorRef<UserMessage>> users) {
    int name;
    ActorRef<UserMessage> user;
    RiskScore symptomScore;
    for (Entry<Integer, ActorRef<UserMessage>> entry : users.entrySet()) {
      name = entry.getKey();
      user = entry.getValue();
      symptomScore = riskScoreFactory.getRiskScore(name);
      user.tell(RiskScoreMessage.builder().score(symptomScore).replyTo(user).build());
    }
  }

  private void sendContacts(Map<Integer, ActorRef<UserMessage>> users) {
    Iterable<Contact> contacts = () -> contactNetwork.contacts().iterator();
    ActorRef<UserMessage> user1, user2;
    for (Contact contact : contacts) {
      user1 = users.get(contact.user1());
      user2 = users.get(contact.user2());
      user1.tell(ContactMessage.builder().replyTo(user2).timestamp(contact.timestamp()).build());
      user2.tell(ContactMessage.builder().replyTo(user1).timestamp(contact.timestamp()).build());
    }
  }

  private void logMetrics() {
    loggables.log(LoggableMetric.KEY, runtimeMetric());
  }

  private Map<Integer, ActorRef<UserMessage>> newUserMap() {
    return new Int2ObjectOpenHashMap<>(contactNetwork.nUsers());
  }

  private Behavior<UserMessage> newUser() {
    return UserBuilder.create()
        .putAllMdc(mdc)
        .addAllLoggable(loggables.loggable())
        .parameters(parameters)
        .clock(clock)
        .cache(cacheFactory.getCache())
        .build();
  }

  private TypedSupplier<LoggableMetric> runtimeMetric() {
    return TypedSupplier.of(RuntimeMetric.class, () -> RuntimeMetric.of(runtime()));
  }

  private float runtime() {
    runtime.stop();
    return (float) (runtime.getNanoTime() / 1e9);
  }
}
