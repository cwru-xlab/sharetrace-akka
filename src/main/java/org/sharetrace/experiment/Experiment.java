package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.events.ContactEvent;
import org.sharetrace.logging.events.ContactsRefreshEvent;
import org.sharetrace.logging.events.CurrentRefreshEvent;
import org.sharetrace.logging.events.ReceiveEvent;
import org.sharetrace.logging.events.SendCachedEvent;
import org.sharetrace.logging.events.SendCurrentEvent;
import org.sharetrace.logging.events.UpdateEvent;
import org.sharetrace.logging.metrics.GraphCycleMetrics;
import org.sharetrace.logging.metrics.GraphEccentricityMetrics;
import org.sharetrace.logging.metrics.GraphScoringMetrics;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.GraphTopologyMetric;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.logging.settings.LoggableSetting;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.CacheParameters;
import org.sharetrace.util.IntervalCache;
import org.slf4j.Logger;

public abstract class Experiment implements Runnable {

  protected static final Logger logger = Logging.settingLogger();
  protected final GraphType graphType;
  protected final long seed;
  protected final int nIterations;
  protected final Loggables loggables;
  protected final Instant referenceTime;
  protected Dataset dataset;
  protected UserParameters userParameters;
  protected CacheParameters cacheParameters;
  protected ExperimentSettings settings;
  protected int iteration;

  protected Experiment(GraphType graphType, int nIterations, long seed) {
    this.graphType = graphType;
    this.nIterations = nIterations;
    this.seed = seed;
    this.referenceTime = newReferenceTime();
    this.loggables = Loggables.create(loggable(), logger);
  }

  protected Instant newReferenceTime() {
    return clock().instant();
  }

  protected Set<Class<? extends Loggable>> loggable() {
    return Set.of(
        // Events
        ContactEvent.class,
        ContactsRefreshEvent.class,
        CurrentRefreshEvent.class,
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
        GraphTopologyMetric.class,
        // Settings
        ExperimentSettings.class);
  }

  protected Clock clock() {
    return Clock.systemUTC();
  }

  @Override
  public void run() {
    IntStream.rangeClosed(1, nIterations).forEach(this::onIteration);
  }

  protected void onIteration(int i) {
    setUpIteration(i);
    Runner.run(newAlgorithm(), "RiskPropagation");
  }

  protected void setUpIteration(int i) {
    iteration = i;
    dataset = newDataset();
    userParameters = newUserParameters();
    cacheParameters = newCacheParameters();
    ExperimentSettings newSettings = newSettings();
    if (!newSettings.equals(settings)) {
      loggables.info(LoggableSetting.KEY, newSettings);
      settings = newSettings;
    }
  }

  protected Behavior<AlgorithmMessage> newAlgorithm() {
    return RiskPropagationBuilder.create()
        .addAllLoggable(loggable())
        .putAllUserMdc(userMdc())
        .contactNetwork(dataset.contactNetwork())
        .parameters(userParameters)
        .clock(clock())
        .riskScoreFactory(dataset)
        .contactTimeFactory(dataset)
        .cacheFactory(cacheFactory())
        .build();
  }

  protected abstract Dataset newDataset();

  protected UserParameters newUserParameters() {
    return UserParameters.builder()
        .sendCoefficient(sendCoefficient())
        .transmissionRate(transmissionRate())
        .timeBuffer(timeBuffer())
        .scoreTtl(scoreTtl())
        .contactTtl(contactTtl())
        .idleTimeout(userTimeout())
        .refreshPeriod(userRefreshPeriod())
        .build();
  }

  protected CacheParameters newCacheParameters() {
    return CacheParameters.builder()
        .interval(cacheInterval())
        .nIntervals(cacheIntervals())
        .refreshPeriod(cacheRefreshPeriod())
        .nLookAhead(cacheLookAhead())
        .build();
  }

  private ExperimentSettings newSettings() {
    return ExperimentSettings.builder()
        .graphType(graphType)
        .nIterations(nIterations)
        .iteration(iteration)
        .seed(seed)
        .userParameters(userParameters)
        .cacheParameters(cacheParameters)
        .build();
  }

  private Map<String, String> userMdc() {
    return Map.of("iteration", String.valueOf(iteration));
  }

  protected CacheFactory<RiskScoreMessage> cacheFactory() {
    return () ->
        IntervalCache.<RiskScoreMessage>builder()
            .nIntervals(cacheParameters.nIntervals())
            .nLookAhead(cacheParameters.nLookAhead())
            .interval(cacheParameters.interval())
            .refreshPeriod(cacheParameters.refreshPeriod())
            .clock(clock())
            .mergeStrategy(this::cacheMerge)
            .build();
  }

  protected float sendCoefficient() {
    return 0.6f;
  }

  protected float transmissionRate() {
    return 0.8f;
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

  protected Duration userTimeout() {
    double nContacts = dataset.contactNetwork().nContacts();
    double minBase = Math.log(1.1d);
    double maxBase = Math.log(10d);
    double decayRate = 1.75E-7;
    double targetBase = Math.max(minBase, maxBase - decayRate * nContacts);
    long timeout = (long) Math.ceil(Math.log(nContacts) / targetBase);
    return Duration.ofSeconds(timeout);
  }

  protected Duration userRefreshPeriod() {
    return Duration.ofHours(1L);
  }

  protected Duration cacheInterval() {
    return Duration.ofDays(1L);
  }

  protected int cacheIntervals() {
    return (int) (2 * defaultTtl().toDays());
  }

  protected Duration cacheRefreshPeriod() {
    return Duration.ofHours(1L);
  }

  protected int cacheLookAhead() {
    return 1;
  }

  protected RiskScoreMessage cacheMerge(RiskScoreMessage oldScore, RiskScoreMessage newScore) {
    float oldValue = oldScore.score().value();
    float newValue = newScore.score().value();
    Instant oldTimestamp = oldScore.score().timestamp();
    Instant newTimestamp = newScore.score().timestamp();
    boolean isHigher = oldValue < newValue;
    boolean isOlder = oldValue == newValue && oldTimestamp.isAfter(newTimestamp);
    return isHigher || isOlder ? newScore : oldScore;
  }

  protected Duration defaultTtl() {
    return Duration.ofDays(14L);
  }
}
