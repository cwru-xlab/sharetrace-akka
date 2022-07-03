package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.data.sampling.RiskScoreSampler;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.TimeSampler;
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
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.CacheParameters;
import org.sharetrace.util.IntervalCache;
import org.sharetrace.util.Range;
import org.slf4j.Logger;
import org.slf4j.MDC;

public abstract class Experiment implements Runnable {

  protected static final Logger logger = Logging.settingLogger();
  protected final Instant referenceTime;
  protected final Sampler<RiskScore> riskScoreSampler;
  protected final Sampler<Instant> contactTimeSampler;
  protected final long seed;
  protected final int nIterations;
  protected final GraphType graphType;
  protected final Loggables loggables;
  protected Dataset dataset;
  protected UserParameters userParameters;
  protected CacheParameters cacheParameters;
  protected String iteration;

  protected Experiment(Builder builder) {
    checkBuilder(builder);
    this.graphType = builder.graphType;
    this.nIterations = builder.nIterations;
    this.seed = builder.seed;
    this.referenceTime = newReferenceTime();
    this.riskScoreSampler = newRiskScoreSampler();
    this.contactTimeSampler = newContactTimeSampler();
    this.loggables = Loggables.create(loggable(), logger);
  }

  private static void checkBuilder(Builder builder) {
    if (!builder.preBuildCalled) {
      throw new IllegalStateException("preBuild() must be called prior to calling build()");
    }
  }

  protected Instant newReferenceTime() {
    return clock().instant();
  }

  protected Sampler<RiskScore> newRiskScoreSampler() {
    Sampler<Instant> timeSampler = newTimeSampler(scoreTtl());
    return RiskScoreSampler.builder().timeSampler(timeSampler).seed(seed).build();
  }

  protected Sampler<Instant> newContactTimeSampler() {
    return newTimeSampler(contactTtl());
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
    Range.of(nIterations).forEach(this::onIteration);
  }

  protected Duration scoreTtl() {
    return defaultTtl();
  }

  protected Duration contactTtl() {
    return defaultTtl();
  }

  protected Duration defaultTtl() {
    return Duration.ofDays(14L);
  }

  protected TimeSampler newTimeSampler(Duration ttl) {
    return TimeSampler.builder().seed(seed).referenceTime(referenceTime).ttl(ttl).build();
  }

  protected void onIteration(double i) {
    setUpIteration();
    runAlgorithm();
  }

  protected void setUpIteration() {
    setIteration();
    addMdc();
    setDatasetAndParameters();
    logDatasetAndSettings();
  }

  protected void runAlgorithm() {
    Runner.run(newAlgorithm(), "RiskPropagation");
  }

  protected void setIteration() {
    iteration = UUID.randomUUID().toString();
  }

  protected void addMdc() {
    mdc().forEach(MDC::put);
  }

  protected void setDatasetAndParameters() {
    setDataset();
    setUserParameters();
    setCacheParameters();
  }

  protected void logDatasetAndSettings() {
    logDataset();
    logSettings();
  }

  protected Behavior<AlgorithmMessage> newAlgorithm() {
    return RiskPropagationBuilder.create()
        .addAllLoggable(loggable())
        .putAllMdc(mdc())
        .contactNetwork(dataset.getContactNetwork())
        .parameters(userParameters)
        .clock(clock())
        .riskScoreFactory(dataset)
        .contactTimeFactory(dataset)
        .cacheFactory(cacheFactory())
        .build();
  }

  private Map<String, String> mdc() {
    return Logging.mdc(iteration, graphType);
  }

  protected abstract void setDataset();

  protected void setUserParameters() {
    userParameters =
        UserParameters.builder()
            .scoreTolerance(scoreTolerance())
            .sendCoefficient(sendCoefficient())
            .transmissionRate(transmissionRate())
            .timeBuffer(timeBuffer())
            .scoreTtl(scoreTtl())
            .contactTtl(contactTtl())
            .idleTimeout(idleTimeout())
            .refreshPeriod(userRefreshPeriod())
            .build();
  }

  protected void setCacheParameters() {
    cacheParameters =
        CacheParameters.builder()
            .interval(cacheInterval())
            .nIntervals(cacheIntervals())
            .refreshPeriod(cacheRefreshPeriod())
            .nLookAhead(cacheLookAhead())
            .build();
  }

  protected void logDataset() {
    dataset.getContactNetwork().logMetrics();
  }

  protected void logSettings() {
    loggables.info(LoggableSetting.KEY, settings());
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

  protected float scoreTolerance() {
    return 0.01f;
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

  protected Duration idleTimeout() {
    double nContacts = dataset.getContactNetwork().nContacts();
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

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .graphType(graphType.toString())
        .iteration(iteration)
        .seed(seed)
        .userParameters(userParameters)
        .cacheParameters(cacheParameters)
        .build();
  }

  protected RiskScoreMessage cacheMerge(RiskScoreMessage oldMsg, RiskScoreMessage newMsg) {
    return isHigher(newMsg, oldMsg) || isApproxEqualAndOlder(newMsg, oldMsg) ? newMsg : oldMsg;
  }

  private boolean isHigher(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return msg1.score().value() - msg2.score().value() > scoreTolerance();
  }

  private boolean isApproxEqualAndOlder(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    RiskScore score1 = msg1.score();
    RiskScore score2 = msg2.score();
    boolean isApproxEqual = Math.abs(score1.value() - score2.value()) < scoreTolerance();
    boolean isOlder = score1.timestamp().isBefore(score2.timestamp());
    return isApproxEqual && isOlder;
  }

  public abstract static class Builder {
    protected GraphType graphType;
    protected int nIterations = 1;
    protected long seed = new Random().nextLong();
    private boolean preBuildCalled = false;

    public Builder graphType(GraphType graphType) {
      this.graphType = Objects.requireNonNull(graphType);
      return this;
    }

    public Builder nIterations(int nIterations) {
      this.nIterations = nIterations;
      return this;
    }

    public Builder seed(long seed) {
      this.seed = seed;
      return this;
    }

    public void preBuild() {
      preBuildCalled = true;
    }

    public abstract Experiment build();
  }
}
