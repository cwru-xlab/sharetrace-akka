package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.logging.ContactEvent;
import org.sharetrace.logging.ContactsRefreshEvent;
import org.sharetrace.logging.CurrentRefreshEvent;
import org.sharetrace.logging.LoggableEvent;
import org.sharetrace.logging.PropagateEvent;
import org.sharetrace.logging.ReceiveEvent;
import org.sharetrace.logging.SendCachedEvent;
import org.sharetrace.logging.SendCurrentEvent;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.Parameters;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.util.IntervalCache;

public abstract class Experiment implements Runnable {

  @Override
  public void run() {
    Runner.run(newAlgorithm());
  }

  protected abstract Dataset<Integer> newDataset(Parameters parameters);

  protected Set<Class<? extends LoggableEvent>> loggable() {
    return Set.of(
        ContactEvent.class,
        CurrentRefreshEvent.class,
        ContactsRefreshEvent.class,
        ReceiveEvent.class,
        PropagateEvent.class,
        SendCurrentEvent.class,
        SendCachedEvent.class);
  }

  protected Behavior<AlgorithmMessage> newAlgorithm() {
    Parameters parameters = parameters();
    Dataset<Integer> dataset = newDataset(parameters);
    return RiskPropagationBuilder.<Integer>create()
        .addAllLoggable(loggable())
        .graph(dataset.graph())
        .parameters(parameters)
        .transmitter(transmitter())
        .clock(clock())
        .scoreFactory(dataset::scoreOf)
        .timeFactory(dataset::contactedAt)
        .cacheFactory(cacheFactory())
        .build();
  }

  protected Supplier<Instant> clock() {
    return Instant::now;
  }

  protected BiFunction<RiskScore, Parameters, RiskScore> transmitter() {
    return (received, parameters) ->
        RiskScore.builder()
            .value(received.value() * parameters.transmissionRate())
            .timestamp(received.timestamp())
            .build();
  }

  protected Parameters parameters() {
    return Parameters.builder()
        .sendTolerance(sendTolerance())
        .transmissionRate(transmissionRate())
        .timeBuffer(timeBuffer())
        .scoreTtl(scoreTtl())
        .contactTtl(contactTtl())
        .idleTimeout(nodeTimeout())
        .refreshRate(nodeRefreshRate())
        .build();
  }

  protected double sendTolerance() {
    return 0.6d;
  }

  protected double transmissionRate() {
    return 0.8d;
  }

  protected Duration scoreTtl() {
    return defaultTtl();
  }

  protected Duration contactTtl() {
    return defaultTtl();
  }

  protected Duration timeBuffer() {
    return Duration.ofDays(2L);
  }

  protected Duration nodeTimeout() {
    return Duration.ofSeconds(5L);
  }

  protected Duration nodeRefreshRate() {
    return Duration.ofHours(1L);
  }

  protected Supplier<IntervalCache<RiskScoreMessage>> cacheFactory() {
    return () ->
        IntervalCache.<RiskScoreMessage>builder()
            .nIntervals(cacheIntervals())
            .nBuffer(cacheBuffer())
            .interval(cacheInterval())
            .refreshRate(cacheRefreshRate())
            .clock(clock())
            .mergeStrategy(this::cacheMerge)
            .prioritizeReads(cachePrioritizeReads())
            .build();
  }

  protected long cacheIntervals() {
    return 1L + defaultTtl().toDays();
  }

  protected long cacheBuffer() {
    return 1L;
  }

  protected Duration cacheInterval() {
    return Duration.ofDays(1L);
  }

  protected Duration cacheRefreshRate() {
    return Duration.ofHours(1L);
  }

  protected boolean cachePrioritizeReads() {
    return false;
  }

  protected Duration defaultTtl() {
    return Duration.ofDays(14L);
  }

  protected RiskScoreMessage cacheMerge(RiskScoreMessage oldScore, RiskScoreMessage newScore) {
    double oldValue = oldScore.score().value();
    double newValue = newScore.score().value();
    Instant oldTimestamp = oldScore.score().timestamp();
    Instant newTimestamp = newScore.score().timestamp();
    boolean isHigher = oldValue < newValue;
    boolean isOlder = oldValue == newValue && oldTimestamp.isAfter(newTimestamp);
    return isHigher || isOlder ? newScore : oldScore;
  }
}
