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
import java.util.Collection;
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
import org.sharetrace.logging.Logger;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.message.AlgorithmMsg;
import org.sharetrace.message.ContactMsg;
import org.sharetrace.message.MsgParams;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.message.RunMsg;
import org.sharetrace.message.UserMsg;
import org.sharetrace.message.UserParams;
import org.sharetrace.util.Iterables;
import org.sharetrace.util.TypedSupplier;
import org.slf4j.MDC;

/**
 * A non-iterative, asynchronous implementation of the ShareTrace algorithm. The objective is to
 * estimate the marginal posterior probability of infection for all individuals in the specified
 * {@link ContactNetwork}. The main steps of the algorithm are as follows:
 *
 * <ol>
 *   <li>Map the {@link ContactNetwork} to a collection {@link User} actors.
 *   <li>For each {@link User}, send it an initial {@link RiskScoreMsg}.
 *   <li>For each pair of {@link User}s that correspond to an edge in the {@link ContactNetwork},
 *       send each a complimentary {@link ContactMsg} that contains the {@link ActorRef} and time of
 *       contact of the other {@link User}.
 *   <li>Terminate once the stopping condition is satisfied. Termination occurs when when all {@link
 *       User}s have stopped passing msgs (default), or a certain amount of time has passed.
 * </ol>
 *
 * @see UserParams
 */
public class RiskPropagation extends AbstractBehavior<AlgorithmMsg> {

  private final Logger logger;
  private final Collection<Class<? extends Loggable>> loggable;
  private final Map<String, String> mdc;
  private final UserParams userParams;
  private final MsgParams msgParams;
  private final ContactNetwork contactNetwork;
  private final Clock clock;
  private final RiskScoreFactory scoreFactory;
  private final CacheFactory<RiskScoreMsg> cacheFactory;
  private final StopWatch runtime;
  private int nStopped;

  private RiskPropagation(
      ActorContext<AlgorithmMsg> ctx,
      Set<Class<? extends Loggable>> loggable,
      Map<String, String> mdc,
      ContactNetwork contactNetwork,
      UserParams userParams,
      MsgParams msgParams,
      Clock clock,
      CacheFactory<RiskScoreMsg> cacheFactory,
      RiskScoreFactory scoreFactory) {
    super(ctx);
    this.loggable = loggable;
    this.logger = Logging.logger(loggable, getContext()::getLog);
    this.mdc = mdc;
    this.contactNetwork = contactNetwork;
    this.userParams = userParams;
    this.msgParams = msgParams;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.runtime = new StopWatch();
    this.nStopped = 0;
  }

  @Builder.Factory
  static Behavior<AlgorithmMsg> riskPropagation(
      ContactNetwork contactNetwork,
      Set<Class<? extends Loggable>> loggable,
      Map<String, String> mdc,
      UserParams userParams,
      MsgParams msgParams,
      Clock clock,
      CacheFactory<RiskScoreMsg> cacheFactory,
      RiskScoreFactory scoreFactory) {
    return Behaviors.setup(
        ctx -> {
          ctx.setLoggerName(Logging.metricsLoggerName());
          return new RiskPropagation(
              ctx,
              loggable,
              mdc,
              contactNetwork,
              userParams,
              msgParams,
              clock,
              cacheFactory,
              scoreFactory);
        });
  }

  @Override
  public Receive<AlgorithmMsg> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMsg.class, this::onRunMessage)
        .onSignal(Terminated.class, this::onTerminateMsg)
        .build();
  }

  private Behavior<AlgorithmMsg> onRunMessage(RunMsg msg) {
    Behavior<AlgorithmMsg> behavior = this;
    if (contactNetwork.numUsers() > 0) {
      Map<Integer, ActorRef<UserMsg>> users = newUsers();
      sendSymptomScores(users);
      runtime.start();
      sendContacts(users);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<Integer, ActorRef<UserMsg>> newUsers() {
    Map<Integer, ActorRef<UserMsg>> users = newUserMap();
    ActorRef<UserMsg> user;
    for (int name : Iterables.fromStream(contactNetwork.users())) {
      user = getContext().spawn(newUser(), String.valueOf(name));
      getContext().watch(user);
      users.put(name, user);
    }
    return users;
  }

  private Map<Integer, ActorRef<UserMsg>> newUserMap() {
    return new Int2ObjectOpenHashMap<>(contactNetwork.numUsers());
  }

  private Behavior<UserMsg> newUser() {
    return UserBuilder.create()
        .putAllMdc(mdc)
        .addAllLoggable(loggable)
        .userParams(userParams)
        .msgParams(msgParams)
        .clock(clock)
        .cache(cacheFactory.cache())
        .build();
  }

  private void sendSymptomScores(Map<Integer, ActorRef<UserMsg>> users) {
    int name;
    ActorRef<UserMsg> user;
    RiskScore symptomScore;
    for (Entry<Integer, ActorRef<UserMsg>> entry : users.entrySet()) {
      name = entry.getKey();
      user = entry.getValue();
      symptomScore = scoreFactory.riskScore(name);
      user.tell(RiskScoreMsg.builder().score(symptomScore).replyTo(user).build());
    }
  }

  private void sendContacts(Map<Integer, ActorRef<UserMsg>> users) {
    ActorRef<UserMsg> user1, user2;
    for (Contact contact : Iterables.fromStream(contactNetwork.contacts())) {
      user1 = users.get(contact.user1());
      user2 = users.get(contact.user2());
      user1.tell(ContactMsg.builder().replyTo(user2).time(contact.time()).build());
      user2.tell(ContactMsg.builder().replyTo(user1).time(contact.time()).build());
    }
  }

  private Behavior<AlgorithmMsg> onTerminateMsg(Terminated msg) {
    Behavior<AlgorithmMsg> behavior = this;
    if (++nStopped == contactNetwork.numUsers()) {
      mdc.forEach(MDC::put);
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private void logMetrics() {
    logger.log(LoggableMetric.KEY, runtimeMetric());
  }

  private TypedSupplier<LoggableMetric> runtimeMetric() {
    return TypedSupplier.of(RuntimeMetric.class, () -> RuntimeMetric.of(runtime()));
  }

  private float runtime() {
    runtime.stop();
    return (float) (runtime.getNanoTime() / 1e9);
  }
}
