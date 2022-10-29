package io.sharetrace.experiment.state;

import io.sharetrace.actor.RiskPropagationBuilder;
import io.sharetrace.data.Dataset;
import io.sharetrace.data.factory.DistributionFactory;
import io.sharetrace.experiment.GraphType;
import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.Logger;
import io.sharetrace.logging.Logging;
import io.sharetrace.logging.setting.ExperimentSettings;
import io.sharetrace.logging.setting.LoggableSetting;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.model.Identifiable;
import io.sharetrace.model.UserParams;
import io.sharetrace.util.CacheParams;
import io.sharetrace.util.Checks;
import io.sharetrace.util.IntervalCache;
import io.sharetrace.util.TypedSupplier;
import io.sharetrace.util.Uid;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.Well512a;
import org.slf4j.MDC;

public final class ExperimentState implements Runnable, Identifiable {

  private final ExperimentContext ctx;
  private final Logger logger;
  private final GraphType graphType;
  private final String id;
  private final Map<String, String> mdc;
  private final CacheParams<RiskScoreMsg> cacheParams;
  private final DistributionFactory scoreValuesFactory;
  private final DistributionFactory scoreTimesFactory;
  private final DistributionFactory contactTimesFactory;
  private final Dataset dataset;
  private final UserParams userParams;

  private ExperimentState(Builder builder) {
    ctx = builder.ctx;
    logger = builder.logger;
    graphType = builder.graphType;
    id = builder.id;
    mdc = builder.mdc;
    cacheParams = builder.cacheParams;
    scoreValuesFactory = builder.scoreValuesFactory;
    scoreTimesFactory = builder.scoreTimesFactory;
    contactTimesFactory = builder.contactTimesFactory;
    userParams = builder.userParams;
    dataset = builder.dataset;
  }

  public static Builder builder(ExperimentContext ctx) {
    return Builder.withDefaults(ctx);
  }

  public void run(int numIterations) {
    IntStream.range(0, numIterations).forEach(x -> toBuilder().build().run());
  }

  @Override
  public void run() {
    logMetricsAndSettings();
    newRiskPropagation().run();
  }

  public Builder toBuilder() {
    return Builder.from(this);
  }

  private void logMetricsAndSettings() {
    mdc.forEach(MDC::put);
    dataset.contactNetwork().logMetrics();
    logger.log(LoggableSetting.KEY, TypedSupplier.of(ExperimentSettings.class, this::settings));
  }

  private Runnable newRiskPropagation() {
    return RiskPropagationBuilder.create()
        .addAllLoggable(ctx.loggable())
        .putAllMdc(mdc)
        .contactNetwork(dataset.contactNetwork())
        .userParams(userParams)
        .clock(ctx.clock())
        .scoreFactory(dataset.scoreFactory())
        .cacheFactory(this::newCache)
        .build();
  }

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .graphType(graphType.toString())
        .networkId(dataset.contactNetwork().id())
        .datasetId(dataset.id())
        .stateId(id)
        .seed(ctx.seed())
        .userParams(userParams)
        .cacheParams(cacheParams)
        .build();
  }

  private IntervalCache<RiskScoreMsg> newCache() {
    return IntervalCache.create(cacheParams);
  }

  @Override
  public String id() {
    return id;
  }

  public Map<String, String> mdc() {
    return Collections.unmodifiableMap(mdc);
  }

  public ExperimentContext context() {
    return ctx;
  }

  public GraphType graphType() {
    return graphType;
  }

  public CacheParams<RiskScoreMsg> cacheParams() {
    return cacheParams;
  }

  public UserParams userParams() {
    return userParams;
  }

  public Dataset dataset() {
    return dataset;
  }

  private enum Setter {
    // Ordered by dependency
    GRAPH_TYPE,
    ID,
    MDC,
    CACHE_PARAMS,
    SCORE_VALUES,
    SCORE_TIMES,
    CONTACT_TIMES,
    DISTRIBUTIONS,
    USER_PARAMS,
    DATASET
  }

  public static class Builder
      implements GraphTypeContext,
          IdContext,
          MdcContext,
          CacheParamsContext,
          DistributionFactoryContext,
          UserParamsContext,
          DatasetContext {

    private final ExperimentContext ctx;
    private final Logger logger;
    private final Map<Setter, Function<? super Builder, Builder>> setters;
    private final Map<String, String> mdc;
    private GraphType graphType;
    private String id;
    private CacheParams<RiskScoreMsg> cacheParams;
    private DistributionFactory scoreValuesFactory;
    private DistributionFactory scoreTimesFactory;
    private DistributionFactory contactTimesFactory;
    private RealDistribution scoreValues;
    private RealDistribution scoreTimes;
    private RealDistribution contactTimes;
    private UserParams userParams;
    private Dataset dataset;

    private Builder(ExperimentContext context) {
      ctx = context;
      logger = Logging.settingsLogger(ctx.loggable());
      setters = newSetters();
      mdc = new Object2ObjectOpenHashMap<>();
    }

    private static Map<Setter, Function<? super Builder, Builder>> newSetters() {
      Map<Setter, Function<? super Builder, Builder>> setters = new EnumMap<>(Setter.class);
      for (Setter setter : Setter.values()) {
        setters.put(setter, Function.identity());
      }
      return setters;
    }

    protected static Builder from(ExperimentState state) {
      return new Builder(state.ctx)
          .graphType(state.graphType)
          .id(ctx -> newId())
          .mdc(ctx -> Logging.mdc(ctx.id()))
          .cacheParams(state.cacheParams)
          .scoreValuesFactory(state.scoreValuesFactory)
          .scoreTimesFactory(state.scoreTimesFactory)
          .contactTimesFactory(state.contactTimesFactory)
          .userParams(state.userParams)
          .dataset(state.dataset);
    }

    public Builder dataset(Dataset dataset) {
      this.dataset = dataset;
      setters.remove(Setter.DATASET);
      return this;
    }

    public Builder userParams(UserParams params) {
      userParams = params;
      setters.remove(Setter.USER_PARAMS);
      return this;
    }

    public Builder contactTimesFactory(DistributionFactory factory) {
      contactTimesFactory = factory;
      setters.remove(Setter.CONTACT_TIMES);
      return this;
    }

    public Builder scoreTimesFactory(DistributionFactory factory) {
      scoreTimesFactory = factory;
      setters.remove(Setter.SCORE_TIMES);
      return this;
    }

    public Builder scoreValuesFactory(DistributionFactory factory) {
      scoreValuesFactory = factory;
      setters.remove(Setter.SCORE_VALUES);
      return this;
    }

    public Builder cacheParams(CacheParams<RiskScoreMsg> params) {
      cacheParams = params;
      setters.remove(Setter.CACHE_PARAMS);
      return this;
    }

    public Builder mdc(Function<MdcContext, Map<String, String>> factory) {
      setters.put(Setter.MDC, factory.andThen(this::mdc));
      return this;
    }

    public Builder id(Function<IdContext, String> factory) {
      setters.put(Setter.ID, factory.andThen(this::id));
      return this;
    }

    public Builder graphType(GraphType graphType) {
      this.graphType = graphType;
      setters.remove(Setter.GRAPH_TYPE);
      return this;
    }

    private static String newId() {
      return Uid.ofIntString();
    }

    public Builder mdc(Map<String, String> mdc) {
      this.mdc.putAll(mdc);
      setters.remove(Setter.MDC);
      return this;
    }

    public Builder id(String id) {
      this.id = id;
      setters.remove(Setter.ID);
      return this;
    }

    protected static Builder withDefaults(ExperimentContext context) {
      return new Builder(context)
          .id(ctx -> newId())
          .mdc(ctx -> Logging.mdc(ctx.id()))
          .cacheParams(ctx -> Defaults.cacheParams())
          .scoreTimesFactory(defaultFactory())
          .scoreValuesFactory(defaultFactory())
          .contactTimesFactory(defaultFactory())
          .userParams(ctx -> Defaults.userParams());
    }

    public Builder userParams(Function<UserParamsContext, UserParams> factory) {
      setters.put(Setter.USER_PARAMS, factory.andThen(this::userParams));
      return this;
    }

    public Builder contactTimesFactory(
        Function<DistributionFactoryContext, DistributionFactory> factory) {
      setters.put(Setter.CONTACT_TIMES, factory.andThen(this::contactTimesFactory));
      return this;
    }

    public Builder scoreValuesFactory(
        Function<DistributionFactoryContext, DistributionFactory> factory) {
      setters.put(Setter.SCORE_VALUES, factory.andThen(this::scoreValuesFactory));
      return this;
    }

    public Builder scoreTimesFactory(
        Function<DistributionFactoryContext, DistributionFactory> factory) {
      setters.put(Setter.SCORE_TIMES, factory.andThen(this::scoreTimesFactory));
      return this;
    }

    public Builder cacheParams(Function<CacheParamsContext, CacheParams<RiskScoreMsg>> factory) {
      setters.put(Setter.CACHE_PARAMS, factory.andThen(this::cacheParams));
      return this;
    }

    private static Function<DistributionFactoryContext, DistributionFactory> defaultFactory() {
      return ctx -> seed -> new UniformRealDistribution(new Well512a(seed), 0d, 1d);
    }

    public Builder graphType(Function<GraphTypeContext, GraphType> factory) {
      setters.put(Setter.GRAPH_TYPE, factory.andThen(this::graphType));
      return this;
    }

    public Builder dataset(Function<DatasetContext, Dataset> factory) {
      setters.put(Setter.DATASET, factory.andThen(this::dataset));
      return this;
    }

    public ExperimentState build() {
      setters.put(Setter.DISTRIBUTIONS, x -> setDistributions());
      setters.values().forEach(setter -> setter.apply(this));
      Checks.isTrue(setters.isEmpty(), "Not all attributes have been set: %s", setters.keySet());
      return new ExperimentState(this);
    }

    private Builder setDistributions() {
      scoreValues = scoreValuesFactory.distribution(ctx.seed());
      scoreTimes = scoreTimesFactory.distribution(ctx.seed());
      contactTimes = contactTimesFactory.distribution(ctx.seed());
      setters.remove(Setter.DISTRIBUTIONS);
      return this;
    }

    @Override
    public UserParams userParams() {
      return userParams;
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
      return Collections.unmodifiableSet(ctx.loggable());
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
    public CacheParams<RiskScoreMsg> cacheParams() {
      return cacheParams;
    }

    @Override
    public RealDistribution scoreValues() {
      return scoreValues;
    }

    @Override
    public RealDistribution scoreTimes() {
      return scoreTimes;
    }

    @Override
    public RealDistribution contactTimes() {
      return contactTimes;
    }
  }
}
