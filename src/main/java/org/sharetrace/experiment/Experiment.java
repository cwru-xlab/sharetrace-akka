package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.IntStream;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.graph.TemporalGraph;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Loggers;
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
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.logging.settings.LoggableSetting;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.NodeParameters;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.util.CacheParameters;
import org.sharetrace.util.IntervalCache;
import org.slf4j.Logger;

public abstract class Experiment implements Runnable {

  private static final Logger logger = Loggers.settingLogger();
  protected final Loggables loggables;
  protected final GraphType graphType;
  protected final long seed;
  protected final int nIterations;
  protected final Instant referenceTime;
  protected NodeParameters nodeParameters;
  private ExperimentSettings previousSettings;

  protected Experiment(GraphType graphType, int nIterations, long seed) {
    this.loggables = Loggables.create(loggable(), logger);
    this.graphType = graphType;
    this.nIterations = nIterations;
    this.seed = seed;
    this.referenceTime = clock().instant();
  }

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
        RuntimeMetric.class,
        // Settings
        ExperimentSettings.class);
  }

  protected Clock clock() {
    return Clock.systemUTC();
  }

  @Override
  public void run() {
    IntStream.range(0, nIterations).forEach(this::onIteration);
  }

  protected void onIteration(int iteration) {
    Behavior<AlgorithmMessage> algorithm = newAlgorithm();
    logSettings(iteration);
    Runner.run(algorithm, "RiskPropagation");
  }

  protected Behavior<AlgorithmMessage> newAlgorithm() {
    Dataset dataset = newDataset();
    return RiskPropagationBuilder.create()
        .addAllLoggable(loggable())
        .graph(dataset.graph())
        .parameters(newNodeParameters(dataset))
        .clock(clock())
        .scoreFactory(dataset)
        .contactTimeFactory(dataset)
        .cacheFactory(cacheFactory())
        .build();
  }

  protected void logSettings(int iteration) {
    ExperimentSettings settings = settings(iteration);
    if (!settings.equals(previousSettings)) {
      loggables.info(LoggableSetting.KEY, LoggableSetting.KEY, settings);
      previousSettings = settings;
    }
  }

  protected abstract Dataset newDataset();

  protected NodeParameters newNodeParameters(Dataset dataset) {
    nodeParameters =
        NodeParameters.builder()
            .sendTolerance(sendTolerance())
            .transmissionRate(transmissionRate())
            .timeBuffer(timeBuffer())
            .scoreTtl(scoreTtl())
            .contactTtl(contactTtl())
            .idleTimeout(computeNodeTimeout(dataset.graph()))
            .refreshRate(nodeRefreshRate())
            .build();
    return nodeParameters;
  }

  protected CacheFactory<RiskScoreMessage> cacheFactory() {
    return () ->
        IntervalCache.<RiskScoreMessage>builder()
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

  private ExperimentSettings settings(int iteration) {
    return ExperimentSettings.builder()
        .nIterations(nIterations)
        .iteration(iteration)
        .seed(seed)
        .nodeParameters(nodeParameters)
        .cacheParameters(cacheParameters())
        .graphType(graphType)
        .build();
  }

  protected Duration computeNodeTimeout(TemporalGraph graph) {
    long timeout = (long) Math.ceil(Math.log(graph.nEdges()) / Math.log(2));
    return Duration.ofSeconds(timeout);
  }

  private CacheParameters cacheParameters() {
    return CacheParameters.builder()
        .prioritizeReads(cachePrioritizeReads())
        .interval(cacheInterval())
        .nIntervals(cacheIntervals())
        .refreshRate(cacheRefreshRate())
        .nLookAhead(cacheLookAhead())
        .build();
  }
}
