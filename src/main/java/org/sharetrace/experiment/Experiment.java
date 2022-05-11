package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.graph.TemporalGraph;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.events.ContactEvent;
import org.sharetrace.logging.events.ContactsRefreshEvent;
import org.sharetrace.logging.events.CurrentRefreshEvent;
import org.sharetrace.logging.events.PropagateEvent;
import org.sharetrace.logging.events.ReceiveEvent;
import org.sharetrace.logging.events.SendCachedEvent;
import org.sharetrace.logging.events.SendCurrentEvent;
import org.sharetrace.logging.events.UpdateEvent;
import org.sharetrace.logging.metrics.GraphCycleMetrics;
import org.sharetrace.logging.metrics.GraphEccentricityMetrics;
import org.sharetrace.logging.metrics.GraphScoringMetrics;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.Parameters;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.util.IntervalCache;

public abstract class Experiment implements Runnable {

  protected final long seed;
  protected final Instant referenceTime;

  protected Experiment(long seed) {
    this.seed = seed;
    this.referenceTime = clock().instant();
  }

  protected Clock clock() {
    return Clock.systemUTC();
  }

  @Override
  public void run() {
    Runner.run(newAlgorithm(), "RiskPropagation");
  }

  protected Behavior<AlgorithmMessage> newAlgorithm() {
    Parameters parameters = parameters();
    Dataset dataset = newDataset(parameters);
    return RiskPropagationBuilder.create()
        .addAllLoggable(loggable())
        .graph(dataset.graph())
        .parameters(parameters)
        .clock(clock())
        .scoreFactory(dataset)
        .contactTimeFactory(dataset)
        .cacheFactory(this::newCache)
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

  protected abstract Dataset newDataset(Parameters parameters);

  protected Set<Class<? extends Loggable>> loggable() {
    return Set.of(
        // Events
        ContactEvent.class,
        ContactsRefreshEvent.class,
        CurrentRefreshEvent.class,
        PropagateEvent.class,
        ReceiveEvent.class,
        SendCachedEvent.class,
        SendCurrentEvent.class,
        UpdateEvent.class,
        // Metrics
        GraphCycleMetrics.class,
        GraphEccentricityMetrics.class,
        GraphScoringMetrics.class,
        GraphSizeMetrics.class,
        RuntimeMetric.class);
  }

  protected IntervalCache<RiskScoreMessage> newCache() {
    return IntervalCache.<RiskScoreMessage>builder()
        .nIntervals(cacheIntervals())
        .nLookAhead(cacheLookAhead())
        .interval(cacheInterval())
        .refreshRate(cacheRefreshRate())
        .clock(clock())
        .mergeStrategy(this::cacheMerge)
        .prioritizeReads(cachePrioritizeReads())
        .build();
  }

  protected double sendTolerance() {
    return 0.6d;
  }

  protected double transmissionRate() {
    return 0.8d;
  }

  protected Duration timeBuffer() {
    return Duration.ofDays(2L);
  }

  protected Duration scoreTtl() {
    return defaultTtl();
  }

  protected Duration contactTtl() {
    return defaultTtl();
  }

  protected Duration nodeTimeout() {
    return Duration.ofSeconds(5L);
  }

  protected Duration nodeRefreshRate() {
    return Duration.ofHours(1L);
  }

  protected long cacheIntervals() {
    return 2L * defaultTtl().toDays();
  }

  protected long cacheLookAhead() {
    return IntervalCache.MIN_LOOK_AHEAD;
  }

  protected Duration cacheInterval() {
    return Duration.ofDays(1L);
  }

  protected Duration cacheRefreshRate() {
    return Duration.ofHours(1L);
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

  protected boolean cachePrioritizeReads() {
    return false;
  }

  protected Duration defaultTtl() {
    return Duration.ofDays(14L);
  }

  protected Duration computeNodeTimeout(TemporalGraph graph) {
    return Duration.ofSeconds((long) Math.ceil(Math.log(graph.nEdges())));
  }
}
