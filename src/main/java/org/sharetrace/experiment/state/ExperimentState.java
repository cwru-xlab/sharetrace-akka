package org.sharetrace.experiment.state;

import akka.actor.typed.Behavior;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.Well512a;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.DistributionFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.data.sampling.RiskScoreSampler;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.TimeSampler;
import org.sharetrace.experiment.ExperimentContext;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Logger;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.logging.settings.LoggableSetting;
import org.sharetrace.message.AlgorithmMsg;
import org.sharetrace.message.MsgParams;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.message.UserParams;
import org.sharetrace.util.CacheParams;
import org.sharetrace.util.IntervalCache;
import org.slf4j.MDC;

public class ExperimentState {

  private final ExperimentContext ctx;
  private final Logger logger;
  private final String id;
  private final GraphType graphType;
  private final Map<String, String> mdc;
  private final DistributionFactory scoreValuesFactory;
  private final DistributionFactory scoreTimesFactory;
  private final DistributionFactory contactTimesFactory;
  private final Dataset dataset;
  private final UserParams userParams;
  private final MsgParams msgParams;
  private final CacheParams<RiskScoreMsg> cacheParams;

  private ExperimentState(Builder builder) {
    ctx = builder.ctx;
    logger = builder.logger;
    graphType = builder.graphType;
    id = builder.id;
    mdc = builder.mdc;
    scoreValuesFactory = builder.scoreValuesFactory;
    scoreTimesFactory = builder.scoreTimesFactory;
    contactTimesFactory = builder.contactTimesFactory;
    dataset = builder.dataset;
    userParams = builder.userParams;
    msgParams = builder.msgParams;
    cacheParams = builder.cacheParams;
  }

  public static Builder builder(ExperimentContext context) {
    return Builder.withDefaults(context);
  }

  public void run() {
    setup();
    runAlgorithm();
  }

  private void setup() {
    mdc.forEach(MDC::put);
    dataset.contactNetwork().logMetrics();
    logger.log(LoggableSetting.KEY, settings());
  }

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .graphType(graphType.toString())
        .stateId(id)
        .seed(ctx.seed())
        .userParams(userParams)
        .msgParams(msgParams)
        .cacheParams(cacheParams)
        .build();
  }

  private void runAlgorithm() {
    Runner.run(newAlgorithm(), "RiskPropagation");
  }

  private Behavior<AlgorithmMsg> newAlgorithm() {
    return RiskPropagationBuilder.create()
        .addAllLoggable(ctx.loggable())
        .putAllMdc(mdc)
        .contactNetwork(dataset.contactNetwork())
        .userParams(userParams)
        .msgParams(msgParams)
        .clock(ctx.clock())
        .scoreFactory(dataset)
        .cacheFactory(cacheFactory())
        .build();
  }

  private CacheFactory<RiskScoreMsg> cacheFactory() {
    return () ->
        IntervalCache.<RiskScoreMsg>builder()
            .clock(ctx.clock())
            .numIntervals(cacheParams.numIntervals())
            .numLookAhead(cacheParams.numLookAhead())
            .interval(cacheParams.interval())
            .refreshPeriod(cacheParams.refreshPeriod())
            .mergeStrategy(cacheParams.mergeStrategy())
            .comparator(RiskScoreMsg::compareTo)
            .build();
  }

  public ExperimentState withNewId() {
    return toBuilder().build();
  }

  public Builder toBuilder() {
    return Builder.from(this);
  }

  public Dataset dataset() {
    return dataset;
  }

  public MsgParams msgParams() {
    return msgParams;
  }

  private enum Setter {
    // Ordered by dependency
    GRAPH_TYPE,
    ID,
    MDC,
    MSG_PARAMS,
    CACHE_PARAMS,
    SCORE_VALUES,
    SCORE_TIMES,
    CONTACT_TIMES,
    DISTRIBUTIONS,
    FACTORIES,
    DATASET,
    USER_PARAMS
  }

  public static class Builder
      implements GraphTypeContext,
          IdContext,
          MdcContext,
          MsgParamsContext,
          CacheParamsContext,
          DistributionFactoryContext,
          DatasetContext,
          UserParamsContext {

    private final ExperimentContext ctx;
    private final Logger logger;
    private final Map<Setter, Function<? super Builder, ?>> setters;
    private final Map<String, String> mdc;
    private GraphType graphType;
    private String id;
    private MsgParams msgParams;
    private CacheParams<RiskScoreMsg> cacheParams;
    private DistributionFactory scoreValuesFactory;
    private DistributionFactory scoreTimesFactory;
    private DistributionFactory contactTimesFactory;
    private RealDistribution scoreValues;
    private RealDistribution scoreTimes;
    private RealDistribution contactTimes;
    private RiskScoreFactory scoreFactory;
    private ContactTimeFactory contactTimeFactory;
    private Dataset dataset;
    private UserParams userParams;

    private Builder(ExperimentContext context) {
      ctx = context;
      logger = Logging.settingsLogger(context.loggable());
      setters = new EnumMap<>(Setter.class);
      mdc = new HashMap<>();
    }

    protected static Builder from(ExperimentState state) {
      return new Builder(state.ctx)
          .graphType(state.graphType)
          .id(ctx -> newId())
          .msgParams(state.msgParams)
          .cacheParams(state.cacheParams)
          .scoreValuesFactory(state.scoreValuesFactory)
          .scoreTimesFactory(state.scoreTimesFactory)
          .contactTimesFactory(state.contactTimesFactory)
          .dataset(state.dataset)
          .userParams(state.userParams);
    }

    private static String newId() {
      return String.valueOf(new Random().nextLong());
    }

    public Builder id(Function<IdContext, String> factory) {
      setters.put(Setter.ID, factory.andThen(this::id));
      return this;
    }

    public Builder id(String id) {
      this.id = Objects.requireNonNull(id);
      setters.remove(Setter.ID);
      return this;
    }

    public Builder msgParams(MsgParams params) {
      msgParams = Objects.requireNonNull(params);
      setters.remove(Setter.MSG_PARAMS);
      return this;
    }

    public Builder cacheParams(CacheParams<RiskScoreMsg> params) {
      cacheParams = Objects.requireNonNull(params);
      setters.remove(Setter.CACHE_PARAMS);
      return this;
    }

    public Builder scoreTimesFactory(DistributionFactory factory) {
      scoreTimesFactory = Objects.requireNonNull(factory);
      setters.remove(Setter.SCORE_TIMES);
      return this;
    }

    public Builder scoreValuesFactory(DistributionFactory factory) {
      scoreValuesFactory = Objects.requireNonNull(factory);
      setters.remove(Setter.SCORE_VALUES);
      return this;
    }

    public Builder contactTimesFactory(DistributionFactory factory) {
      contactTimesFactory = Objects.requireNonNull(factory);
      setters.remove(Setter.CONTACT_TIMES);
      return this;
    }

    public Builder userParams(UserParams parameters) {
      userParams = Objects.requireNonNull(parameters);
      setters.remove(Setter.USER_PARAMS);
      return this;
    }

    public Builder graphType(GraphType graphType) {
      this.graphType = Objects.requireNonNull(graphType);
      setters.remove(Setter.GRAPH_TYPE);
      return this;
    }

    public Builder dataset(Dataset dataset) {
      this.dataset = Objects.requireNonNull(dataset);
      setters.remove(Setter.DATASET);
      return this;
    }

    protected static Builder withDefaults(ExperimentContext context) {
      return new Builder(context)
          .id(ctx -> newId())
          .mdc(ctx -> Logging.mdc(ctx.id(), ctx.graphType()))
          .msgParams(ctx -> Defaults.msgParams())
          .cacheParams(ctx -> Defaults.cacheParams())
          .scoreTimesFactory(defaultFactory())
          .scoreValuesFactory(defaultFactory())
          .contactTimesFactory(defaultFactory())
          .userParams(ctx -> Defaults.userParams(ctx.dataset()));
    }

    private static Function<DistributionFactoryContext, DistributionFactory> defaultFactory() {
      return ctx -> seed -> new UniformRealDistribution(new Well512a(seed), 0d, 1d);
    }

    public Builder mdc(Function<MdcContext, Map<String, String>> factory) {
      setters.put(Setter.MDC, factory.andThen(this::mdc));
      return this;
    }

    public Builder mdc(Map<String, String> mdc) {
      this.mdc.putAll(Objects.requireNonNull(mdc));
      setters.remove(Setter.MDC);
      return this;
    }

    public Builder msgParams(Function<MsgParamsContext, MsgParams> factory) {
      setters.put(Setter.MSG_PARAMS, factory.andThen(this::msgParams));
      return this;
    }

    public Builder cacheParams(Function<CacheParamsContext, CacheParams<RiskScoreMsg>> factory) {
      setters.put(Setter.CACHE_PARAMS, factory.andThen(this::cacheParams));
      return this;
    }

    public Builder scoreTimesFactory(
        Function<DistributionFactoryContext, DistributionFactory> factory) {
      setters.put(Setter.SCORE_TIMES, factory.andThen(this::scoreTimesFactory));
      return this;
    }

    public Builder scoreValuesFactory(
        Function<DistributionFactoryContext, DistributionFactory> factory) {
      setters.put(Setter.SCORE_VALUES, factory.andThen(this::scoreValuesFactory));
      return this;
    }

    public Builder contactTimesFactory(
        Function<DistributionFactoryContext, DistributionFactory> factory) {
      setters.put(Setter.CONTACT_TIMES, factory.andThen(this::contactTimesFactory));
      return this;
    }

    public Builder userParams(Function<UserParamsContext, UserParams> factory) {
      setters.put(Setter.USER_PARAMS, factory.andThen(this::userParams));
      return this;
    }

    public ExperimentState build() {
      setters.put(Setter.DISTRIBUTIONS, x -> setDistributions());
      setters.put(Setter.FACTORIES, x -> setFactories());
      setters.values().forEach(setter -> setter.apply(this));
      return new ExperimentState(this);
    }

    private Void setDistributions() {
      scoreValues = scoreValuesFactory.distribution(ctx.seed());
      scoreTimes = scoreTimesFactory.distribution(ctx.seed());
      contactTimes = contactTimesFactory.distribution(ctx.seed());
      return null;
    }

    private Void setFactories() {
      Sampler<RiskScore> scoreSampler = newScoreSampler();
      scoreFactory = RiskScoreFactory.from(scoreSampler::sample);
      Sampler<Instant> contactTimeSampler = newContactTimeSampler();
      contactTimeFactory = ContactTimeFactory.from(contactTimeSampler::sample);
      return null;
    }

    private Sampler<RiskScore> newScoreSampler() {
      return RiskScoreSampler.builder()
          .values(scoreValues)
          .timeSampler(newTimeSampler(scoreTimes, msgParams.scoreTtl()))
          .build();
    }

    private Sampler<Instant> newTimeSampler(RealDistribution lookBacks, Duration maxLookBack) {
      return TimeSampler.builder()
          .lookBacks(lookBacks)
          .maxLookBack(maxLookBack)
          .refTime(ctx.refTime())
          .build();
    }

    private Sampler<Instant> newContactTimeSampler() {
      return newTimeSampler(contactTimes, msgParams.contactTtl());
    }

    @Override
    public Instant refTime() {
      return ctx.refTime();
    }

    @Override
    public Clock clock() {
      return ctx.clock();
    }

    @Override
    public long seed() {
      return ctx.seed();
    }

    @Override
    public Set<Class<? extends Loggable>> loggable() {
      return ctx.loggable();
    }

    @Override
    public GraphType graphType() {
      return graphType;
    }

    @Override
    public String id() {
      return id;
    }

    @Override
    public Map<String, String> mdc() {
      return Collections.unmodifiableMap(mdc);
    }

    @Override
    public MsgParams msgParams() {
      return msgParams;
    }

    @Override
    public CacheParams<RiskScoreMsg> cacheParams() {
      return cacheParams;
    }

    @Override
    public RiskScoreFactory scoreFactory() {
      return scoreFactory;
    }

    @Override
    public ContactTimeFactory contactTimeFactory() {
      return contactTimeFactory;
    }

    @Override
    public Dataset dataset() {
      return dataset;
    }

    public Builder graphType(Function<GraphTypeContext, GraphType> factory) {
      setters.put(Setter.GRAPH_TYPE, factory.andThen(this::graphType));
      return this;
    }

    public Builder dataset(Function<DatasetContext, Dataset> factory) {
      setters.put(Setter.DATASET, factory.andThen(this::dataset));
      return this;
    }
  }
}
