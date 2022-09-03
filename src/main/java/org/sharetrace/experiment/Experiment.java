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
import javax.annotation.Nullable;
import org.apache.commons.math3.distribution.RealDistribution;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
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
import org.sharetrace.util.Checks;
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
    this.riskScoreSampler = newRiskScoreSampler(builder);
    this.contactTimeSampler = newContactTimeSampler(builder);
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

  private Sampler<RiskScore> newRiskScoreSampler(Builder builder) {
    RiskScoreSampler.Builder samplerBuilder = RiskScoreSampler.builder();
    if (builder.scoreValueDistribution != null) {
      samplerBuilder.valueDistribution(builder.scoreValueDistribution);
    }
    Sampler<Instant> timeSampler = newTimeSampler(builder.scoreTimeTtlDistribution, scoreTtl());
    return samplerBuilder.seed(seed).timeSampler(timeSampler).build();
  }

  private Sampler<Instant> newContactTimeSampler(Builder builder) {
    return newTimeSampler(builder.contactTimeTtlDistribution, contactTtl());
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

  private Sampler<Instant> newTimeSampler(
      @Nullable RealDistribution ttlDistribution, Duration ttl) {
    TimeSampler.Builder builder = TimeSampler.builder();
    if (ttlDistribution != null) {
      builder.ttlDistribution(ttlDistribution);
    }
    return builder.ttl(ttl).seed(seed).referenceTime(referenceTime).build();
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

  @Override
  public void run() {
    Range.of(nIterations).forEach(x -> onIteration());
  }

  protected void onIteration() {
    setUpIteration();
    runAlgorithm();
  }

  protected void setUpIteration() {
    setIteration();
    addMdc();
    setDataset();
    setUserParameters();
    setCacheParameters();
    logDataset();
    logSettings();
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

  protected abstract void setDataset();

  protected Behavior<AlgorithmMessage> newAlgorithm() {
    return RiskPropagationBuilder.create()
        .addAllLoggable(loggable())
        .putAllMdc(mdc())
        .contactNetwork(dataset.getContactNetwork())
        .parameters(userParameters)
        .clock(clock())
        .riskScoreFactory(dataset)
        .cacheFactory(cacheFactory())
        .build();
  }

  private Map<String, String> mdc() {
    return Logging.mdc(iteration, graphType);
  }

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
    loggables.log(LoggableSetting.KEY, settings());
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
            .comparator(RiskScoreMessage.comparator())
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
    double minBase = Math.log(1.1);
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
    return Math.toIntExact(2 * defaultTtl().toDays());
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
    // Simpler to check for higher value first.
    // Most will likely not be older, which avoids checking for approximate equality.
    return isHigher(newMsg, oldMsg) || (isOlder(newMsg, oldMsg) && isApproxEqual(newMsg, oldMsg))
        ? newMsg
        : oldMsg;
  }

  private static boolean isHigher(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return msg1.score().value() > msg2.score().value();
  }

  private static boolean isOlder(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return msg1.score().timestamp().isBefore(msg2.score().timestamp());
  }

  private boolean isApproxEqual(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return Math.abs(msg1.score().value() - msg2.score().value()) < scoreTolerance();
  }

  protected RiskScoreFactory riskScoreFactory() {
    return RiskScoreFactory.fromSupplier(riskScoreSampler::sample);
  }

  protected ContactTimeFactory contactTimeFactory() {
    return ContactTimeFactory.fromSupplier(contactTimeSampler::sample);
  }

  public abstract static class Builder {

    protected GraphType graphType;
    protected int nIterations = 1;
    protected long seed = new Random().nextLong();
    private RealDistribution scoreValueDistribution;
    private RealDistribution scoreTimeTtlDistribution;
    private RealDistribution contactTimeTtlDistribution;
    private boolean preBuildCalled = false;

    public Builder graphType(GraphType graphType) {
      this.graphType = graphType;
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

    public Builder scoreValueDistribution(RealDistribution scoreValueDistribution) {
      this.scoreValueDistribution = scoreValueDistribution;
      return this;
    }

    public Builder scoreTimeTtlDistribution(RealDistribution scoreTimeTtlDistribution) {
      this.scoreTimeTtlDistribution = scoreTimeTtlDistribution;
      return this;
    }

    public Builder contactTimeTtlDistribution(RealDistribution contactTimeTtlDistribution) {
      this.contactTimeTtlDistribution = contactTimeTtlDistribution;
      return this;
    }

    public abstract Experiment build();

    protected void preBuild() {
      Objects.requireNonNull(graphType);
      Checks.greaterThan(nIterations, 0, "nIterations");
      preBuildCalled = true;
    }
  }
}
