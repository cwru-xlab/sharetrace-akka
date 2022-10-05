package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import io.sharetrace.data.factory.CacheFactory;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.Contact;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.Logger;
import io.sharetrace.logging.Logging;
import io.sharetrace.logging.metric.CreateUsersRuntime;
import io.sharetrace.logging.metric.LoggableMetric;
import io.sharetrace.logging.metric.MsgPassingRuntime;
import io.sharetrace.logging.metric.RiskPropRuntime;
import io.sharetrace.logging.metric.SendContactsRuntime;
import io.sharetrace.logging.metric.SendScoresRuntime;
import io.sharetrace.message.AlgorithmMsg;
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.RunMsg;
import io.sharetrace.message.TimedOutMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.UserParams;
import io.sharetrace.util.CacheParams;
import io.sharetrace.util.TypedSupplier;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.time.Clock;
import java.util.BitSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.commons.lang3.time.StopWatch;
import org.immutables.builder.Builder;
import org.slf4j.MDC;

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

  private final Logger logger;
  private final Props userProps;
  private final Set<Class<? extends Loggable>> loggable;
  private final Map<String, String> mdc;
  private final UserParams userParams;
  private final ContactNetwork contactNetwork;
  private final int numUsers;
  private final Clock clock;
  private final RiskScoreFactory scoreFactory;
  private final CacheFactory<RiskScoreMsg> cacheFactory;
  private final Timer timer;
  private final BitSet stopped;

  private RiskPropagation(
      ActorContext<AlgorithmMsg> ctx,
      Set<Class<? extends Loggable>> loggable,
      Map<String, String> mdc,
      ContactNetwork contactNetwork,
      UserParams userParams,
      Clock clock,
      CacheFactory<RiskScoreMsg> cacheFactory,
      RiskScoreFactory scoreFactory) {
    super(ctx);
    this.loggable = loggable;
    this.logger = Logging.logger(loggable, getContext()::getLog);
    this.userProps = DispatcherSelector.fromConfig("user-dispatcher");
    this.mdc = mdc;
    this.contactNetwork = contactNetwork;
    this.numUsers = contactNetwork.users().size();
    this.userParams = userParams;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.timer = new Timer();
    this.stopped = new BitSet(numUsers);
  }

  @Builder.Factory
  static Runnable riskPropagation(
      ContactNetwork contactNetwork,
      Set<Class<? extends Loggable>> loggable,
      Map<String, String> mdc,
      UserParams userParams,
      Clock clock,
      CacheFactory<RiskScoreMsg> cacheFactory,
      RiskScoreFactory scoreFactory) {
    Behavior<AlgorithmMsg> behavior =
        Behaviors.setup(
            ctx -> {
              ctx.setLoggerName(Logging.METRICS_LOGGER_NAME);
              return new RiskPropagation(
                  ctx,
                  loggable,
                  mdc,
                  contactNetwork,
                  userParams,
                  clock,
                  cacheFactory,
                  scoreFactory);
            });
    return Algorithm.of(behavior, RiskPropagation.class.getSimpleName());
  }

  private static long milli(long nanos) {
    return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public Receive<AlgorithmMsg> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMsg.class, this::handle)
        .onMessage(TimedOutMsg.class, this::handle)
        .build();
  }

  private long milli(Class<?> metric) {
    return milli(timer.nanos(metric));
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

  private Map<Integer, ActorRef<UserMsg>> newUsers() {
    Map<Integer, ActorRef<UserMsg>> users = new Int2ObjectOpenHashMap<>(numUsers);
    ActorRef<UserMsg> user;
    for (int name : contactNetwork.users()) {
      // Timeout IDs must be 0-based contiguous to use with 'stopped' BitSet.
      user = getContext().spawn(newUser(name), String.valueOf(name), userProps);
      getContext().watch(user);
      users.put(name, user);
    }
    return users;
  }

  private Behavior<UserMsg> newUser(int timeoutId) {
    return UserBuilder.create()
        .riskProp(getContext().getSelf())
        .timeoutId(timeoutId)
        .putAllMdc(mdc)
        .addAllLoggable(loggable)
        .userParams(userParams)
        .clock(clock)
        .cache(cacheFactory.newCache())
        .build();
  }

  private void sendSymptomScores(Map<Integer, ActorRef<UserMsg>> users) {
    int name;
    ActorRef<UserMsg> user;
    RiskScore symptomScore;
    // Assumes at-least-once message delivery to the user actors.
    for (Entry<Integer, ActorRef<UserMsg>> entry : users.entrySet()) {
      name = entry.getKey();
      user = entry.getValue();
      symptomScore = scoreFactory.riskScore(name);
      user.tell(RiskScoreMsg.of(symptomScore, user));
    }
  }

  private void sendContacts(Map<Integer, ActorRef<UserMsg>> users) {
    ActorRef<UserMsg> user1, user2;
    // Assumes at-least-once message delivery to the user actors.
    for (Contact contact : contactNetwork.contacts()) {
      user1 = users.get(contact.user1());
      user2 = users.get(contact.user2());
      user1.tell(ContactMsg.of(user2, contact.time()));
      user2.tell(ContactMsg.of(user1, contact.time()));
    }
  }

  private Behavior<AlgorithmMsg> handle(TimedOutMsg msg) {
    Behavior<AlgorithmMsg> behavior = this;
    // Assumes at-least-once message delivery from the user actors.
    stopped.set(msg.user());
    if (stopped.cardinality() == numUsers) {
      timer.stop();
      mdc.forEach(MDC::put);
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private void logMetrics() {
    logMetric(CreateUsersRuntime.class, this::createRuntime);
    logMetric(SendScoresRuntime.class, this::scoresRuntime);
    logMetric(SendContactsRuntime.class, this::contactsRuntime);
    logMetric(RiskPropRuntime.class, this::riskPropRuntime);
    logMetric(MsgPassingRuntime.class, this::msgPassingRuntime);
  }

  private <T extends Loggable> void logMetric(Class<T> type, Supplier<T> supplier) {
    logger.log(LoggableMetric.KEY, TypedSupplier.of(type, supplier));
  }

  private CreateUsersRuntime createRuntime() {
    return CreateUsersRuntime.of(milli(CreateUsersRuntime.class));
  }

  private SendScoresRuntime scoresRuntime() {
    return SendScoresRuntime.of(milli(SendScoresRuntime.class));
  }

  private SendContactsRuntime contactsRuntime() {
    return SendContactsRuntime.of(milli(SendContactsRuntime.class));
  }

  private RiskPropRuntime riskPropRuntime() {
    return RiskPropRuntime.of(milli(RiskPropRuntime.class));
  }

  private MsgPassingRuntime msgPassingRuntime() {
    long total = timer.nanos(RiskPropRuntime.class);
    long exclude = timer.nanos(SendScoresRuntime.class) + timer.nanos(CreateUsersRuntime.class);
    return MsgPassingRuntime.of(milli(total - exclude));
  }

  private static final class Timer extends StopWatch {

    private final Map<Class<?>, Long> runtimes = new Object2LongOpenHashMap<>();

    public void time(Runnable task, Class<?> metric) {
      time(Executors.callable(task), metric);
    }

    public <R> R time(Callable<R> task, Class<?> metric) {
      long start, stop;
      R result;
      try {
        start = getNanoTime();
        result = task.call();
        stop = getNanoTime();
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
      runtimes.put(metric, stop - start);
      return result;
    }

    public long nanos(Class<?> metric) {
      return runtimes.computeIfAbsent(metric, x -> getNanoTime());
    }
  }
}
