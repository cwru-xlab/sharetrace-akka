package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import com.google.common.math.DoubleMath;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.Well512a;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.PdfFactory;
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
import org.sharetrace.logging.metrics.CycleMetrics;
import org.sharetrace.logging.metrics.EccentricityMetrics;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.logging.metrics.ScoringMetrics;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.logging.metrics.TopologyMetric;
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
    graphType = builder.graphType;
    nIterations = builder.nIterations;
    seed = builder.seed;
    referenceTime = newReferenceTime();
    riskScoreSampler = newRiskScoreSampler(builder);
    contactTimeSampler = newContactTimeSampler(builder);
    loggables = Loggables.create(loggable(), logger);
  }

  protected Instant newReferenceTime() {
    return clock().instant();
  }

  private Sampler<RiskScore> newRiskScoreSampler(Builder builder) {
    return RiskScoreSampler.builder()
        .valueDistribution(builder.scoreValuePdf)
        .timeSampler(newTimeSampler(builder.scoreTimeTtlPdf, scoreTtl()))
        .build();
  }

  private Sampler<Instant> newContactTimeSampler(Builder builder) {
    return newTimeSampler(builder.contactTimeTtlPdf, contactTtl());
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
        CycleMetrics.class,
        EccentricityMetrics.class,
        ScoringMetrics.class,
        SizeMetrics.class,
        RuntimeMetric.class,
        TopologyMetric.class,
        // Settings
        ExperimentSettings.class);
  }

  protected Clock clock() {
    return Clock.systemUTC();
  }

  private Sampler<Instant> newTimeSampler(RealDistribution ttlDistribution, Duration ttl) {
    return TimeSampler.builder()
        .ttlDistribution(ttlDistribution)
        .ttl(ttl)
        .referenceTime(referenceTime)
        .build();
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

  protected CacheFactory<RiskScoreMessage> cacheFactory() {
    return () ->
        IntervalCache.<RiskScoreMessage>builder()
            .nIntervals(cacheParameters.nIntervals())
            .nLookAhead(cacheParameters.nLookAhead())
            .interval(cacheParameters.interval())
            .refreshPeriod(cacheParameters.refreshPeriod())
            .clock(clock())
            .mergeStrategy(this::cacheMerge)
            .comparator(RiskScoreMessage::compareTo)
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
    return DoubleMath.fuzzyEquals(msg1.score().value(), msg2.score().value(), scoreTolerance());
  }

  protected RiskScoreFactory riskScoreFactory() {
    return RiskScoreFactory.fromSupplier(riskScoreSampler::sample);
  }

  protected ContactTimeFactory contactTimeFactory() {
    return ContactTimeFactory.fromSupplier(contactTimeSampler::sample);
  }

  public static class Builder {

    protected GraphType graphType;
    protected int nIterations = 1;
    protected long seed = new Random().nextLong();
    private PdfFactory scoreValuePdfFactory = defaultPdfFactory();
    private PdfFactory scoreTimeTtlPdfFactory = defaultPdfFactory();
    private PdfFactory contactTimeTtlPdfFactory = defaultPdfFactory();
    private RealDistribution scoreValuePdf;
    private RealDistribution scoreTimeTtlPdf;
    private RealDistribution contactTimeTtlPdf;

    public Builder graphType(GraphType graphType) {
      this.graphType = graphType;
      return this;
    }

    public Builder nIterations(int nIterations) {
      this.nIterations = Checks.greaterThan(nIterations, 0, "nIterations");
      return this;
    }

    public Builder seed(long seed) {
      this.seed = seed;
      return this;
    }

    public Builder scoreValuePdfFactory(PdfFactory scoreValuePdfFactory) {
      this.scoreValuePdfFactory = Objects.requireNonNull(scoreValuePdfFactory);
      return this;
    }

    public Builder scoreTimeTtlPdfFactory(PdfFactory scoreTimeTtlPdfFactory) {
      this.scoreTimeTtlPdfFactory = Objects.requireNonNull(scoreTimeTtlPdfFactory);
      return this;
    }

    public Builder contactTimeTtlPdfFactory(PdfFactory contactTimeTtlPdfFactory) {
      this.contactTimeTtlPdfFactory = Objects.requireNonNull(contactTimeTtlPdfFactory);
      return this;
    }

    public Experiment build() {
      throw new UnsupportedOperationException();
    }

    protected void checkFields() {
      Objects.requireNonNull(graphType);
      scoreValuePdf = scoreValuePdfFactory.getPdf(seed);
      scoreTimeTtlPdf = scoreTimeTtlPdfFactory.getPdf(seed);
      contactTimeTtlPdf = contactTimeTtlPdfFactory.getPdf(seed);
    }

    private PdfFactory defaultPdfFactory() {
      return s -> new UniformRealDistribution(new Well512a(s), 0d, 1d);
    }
  }
}
