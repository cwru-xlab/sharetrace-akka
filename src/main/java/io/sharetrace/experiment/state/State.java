package io.sharetrace.experiment.state;

import io.sharetrace.actor.RiskPropagationBuilder;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.data.factory.DistributionFactory;
import io.sharetrace.graph.GraphType;
import io.sharetrace.model.Identifiable;
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.util.Checks;
import io.sharetrace.util.Identifiers;
import io.sharetrace.util.cache.CacheParameters;
import io.sharetrace.util.cache.IntervalCache;
import io.sharetrace.util.logging.Loggable;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.TypedLogger;
import io.sharetrace.util.logging.setting.ExperimentSettings;
import io.sharetrace.util.logging.setting.LoggableSetting;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

public final class State
    implements Runnable,
        Identifiable,
        GraphTypeContext,
        IdContext,
        CacheParametersContext,
        DistributionFactoryContext,
        UserParametersContext,
        DatasetContext {

  private static final TypedLogger<LoggableSetting> LOGGER = Logging.settingsLogger();
  private final Context context;
  private final GraphType graphType;
  private final String id;
  private final CacheParameters<RiskScoreMessage> cacheParameters;
  private final DistributionFactory scoreValuesFactory;
  private final DistributionFactory scoreTimesFactory;
  private final DistributionFactory contactTimesFactory;
  private final Dataset dataset;
  private final UserParameters userParameters;

  private State(Builder builder) {
    context = builder.context;
    graphType = builder.graphType;
    id = builder.id;
    cacheParameters = builder.cacheParameters;
    scoreValuesFactory = builder.scoreValuesFactory;
    scoreTimesFactory = builder.scoreTimesFactory;
    contactTimesFactory = builder.contactTimesFactory;
    userParameters = builder.userParameters;
    dataset = builder.dataset;
  }

  public static Builder builder() {
    return builder(Defaults.context());
  }

  public static Builder builder(Context ctx) {
    return Builder.withDefaults(ctx);
  }

  public void run(int numIterations) {
    IntStream.range(0, numIterations).forEach(x -> toBuilder().build().run());
  }

  @Override
  public void run() {
    setUpLogging();
    logMetricsAndSettings();
    runAlgorithm();
  }

  public Builder toBuilder() {
    return Builder.from(this);
  }

  private void setUpLogging() {
    Logging.setMdc(id);
    Logging.enable(context.loggable());
  }

  private void logMetricsAndSettings() {
    dataset.contactNetwork().logMetrics();
    LOGGER.log(LoggableSetting.KEY, ExperimentSettings.class, this::settings);
  }

  private void runAlgorithm() {
    RiskPropagationBuilder.create()
        .dataset(dataset)
        .userParameters(userParameters)
        .clock(context.clock())
        .cacheFactory(() -> IntervalCache.create(cacheParameters))
        .build()
        .run();
  }

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .graphType(graphType.toString())
        .networkId(dataset.contactNetwork().id())
        .datasetId(dataset.id())
        .stateId(id)
        .seed(context.seed())
        .userParameters(userParameters)
        .cacheParameters(cacheParameters)
        .build();
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public Instant refTime() {
    return context.refTime();
  }

  @Override
  public Clock clock() {
    return context.clock();
  }

  @Override
  public long seed() {
    return context.seed();
  }

  @Override
  public Set<Class<? extends Loggable>> loggable() {
    return context.loggable();
  }

  @Override
  public GraphType graphType() {
    return graphType;
  }

  @Override
  public CacheParameters<RiskScoreMessage> cacheParameters() {
    return cacheParameters;
  }

  @Override
  public UserParameters userParameters() {
    return userParameters;
  }

  @Override
  public RealDistribution scoreValues() {
    return scoreTimesFactory.get(seed());
  }

  @Override
  public RealDistribution scoreTimes() {
    return scoreTimesFactory.get(seed());
  }

  @Override
  public RealDistribution contactTimes() {
    return contactTimesFactory.get(seed());
  }

  public Context context() {
    return context;
  }

  public Dataset dataset() {
    return dataset;
  }

  public static class Builder
      implements GraphTypeContext,
          IdContext,
          CacheParametersContext,
          DistributionFactoryContext,
          UserParametersContext,
          DatasetContext {

    private final Context context;
    private final Map<Setter, Function<? super Builder, ?>> setters;
    private GraphType graphType;
    private String id;
    private CacheParameters<RiskScoreMessage> cacheParameters;
    private DistributionFactory scoreValuesFactory;
    private DistributionFactory scoreTimesFactory;
    private DistributionFactory contactTimesFactory;
    private RealDistribution scoreValues;
    private RealDistribution scoreTimes;
    private RealDistribution contactTimes;
    private UserParameters userParameters;
    private Dataset dataset;

    private Builder(Context context) {
      this.context = context;
      setters = newSetters();
    }

    private static Map<Setter, Function<? super Builder, ?>> newSetters() {
      Map<Setter, Function<? super Builder, ?>> setters = new EnumMap<>(Setter.class);
      for (Setter setter : Setter.values()) {
        setters.put(setter, Function.identity());
      }
      return setters;
    }

    private static Builder withDefaults(Context context) {
      return new Builder(context)
          .id(ctx -> newId())
          .CacheParameters(ctx -> Defaults.cacheParameters())
          .scoreTimesFactory(defaultFactory())
          .scoreValuesFactory(defaultFactory())
          .contactTimesFactory(defaultFactory())
          .userParameters(ctx -> Defaults.userParameters());
    }

    private static String newId() {
      return Identifiers.ofIntString();
    }

    private static Function<DistributionFactoryContext, DistributionFactory> defaultFactory() {
      return ctx -> seed -> new UniformRealDistribution(Defaults.rng(seed), 0d, 1d);
    }

    private static Builder from(State state) {
      return new Builder(state.context)
          .graphType(state.graphType)
          .id(ctx -> newId())
          .CacheParameters(state.cacheParameters)
          .scoreValuesFactory(state.scoreValuesFactory)
          .scoreTimesFactory(state.scoreTimesFactory)
          .contactTimesFactory(state.contactTimesFactory)
          .userParameters(state.userParameters)
          .dataset(state.dataset);
    }

    public Builder userParameters(Function<UserParametersContext, UserParameters> factory) {
      setters.put(Setter.USER_PARAMS, factory.andThen(this::userParameters));
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

    public Builder CacheParameters(
        Function<CacheParametersContext, CacheParameters<RiskScoreMessage>> factory) {
      setters.put(Setter.CACHE_PARAMS, factory.andThen(this::CacheParameters));
      return this;
    }

    public Builder id(Function<IdContext, String> factory) {
      setters.put(Setter.ID, factory.andThen(this::id));
      return this;
    }

    public Builder userParameters(UserParameters params) {
      userParameters = params;
      setters.remove(Setter.USER_PARAMS);
      return this;
    }

    public Builder contactTimesFactory(DistributionFactory factory) {
      contactTimesFactory = factory;
      setters.remove(Setter.CONTACT_TIMES);
      return this;
    }

    public Builder scoreValuesFactory(DistributionFactory factory) {
      scoreValuesFactory = factory;
      setters.remove(Setter.SCORE_VALUES);
      return this;
    }

    public Builder scoreTimesFactory(DistributionFactory factory) {
      scoreTimesFactory = factory;
      setters.remove(Setter.SCORE_TIMES);
      return this;
    }

    public Builder CacheParameters(CacheParameters<RiskScoreMessage> params) {
      cacheParameters = params;
      setters.remove(Setter.CACHE_PARAMS);
      return this;
    }

    public Builder id(String id) {
      this.id = id;
      setters.remove(Setter.ID);
      return this;
    }

    public Builder dataset(Dataset dataset) {
      this.dataset = dataset;
      setters.remove(Setter.DATASET);
      return this;
    }

    public Builder graphType(GraphType graphType) {
      this.graphType = graphType;
      setters.remove(Setter.GRAPH_TYPE);
      return this;
    }

    public Builder graphType(Function<GraphTypeContext, GraphType> factory) {
      setters.put(Setter.GRAPH_TYPE, factory.andThen(this::graphType));
      return this;
    }

    public Builder dataset(Function<DatasetContext, Dataset> factory) {
      setters.put(Setter.DATASET, factory.andThen(this::dataset));
      return this;
    }

    public State build() {
      setters.put(Setter.DISTRIBUTIONS, x -> setDistributions());
      setters.values().forEach(setter -> setter.apply(this));
      Checks.checkState(
          setters.isEmpty(), "Not all attributes have been set: %s", setters.keySet());
      return new State(this);
    }

    private Builder setDistributions() {
      scoreValues = scoreValuesFactory.get(context.seed());
      scoreTimes = scoreTimesFactory.get(context.seed());
      contactTimes = contactTimesFactory.get(context.seed());
      setters.remove(Setter.DISTRIBUTIONS);
      return this;
    }

    @Override
    public UserParameters userParameters() {
      return userParameters;
    }

    @Override
    public Instant refTime() {
      return context.refTime();
    }

    @Override
    public Clock clock() {
      return context.clock();
    }

    @Override
    public long seed() {
      return context.seed();
    }

    @Override
    public Set<Class<? extends Loggable>> loggable() {
      return context.loggable();
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
    public CacheParameters<RiskScoreMessage> cacheParameters() {
      return cacheParameters;
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

    private enum Setter {
      // Ordered by dependency
      GRAPH_TYPE,
      ID,
      CACHE_PARAMS,
      SCORE_VALUES,
      SCORE_TIMES,
      CONTACT_TIMES,
      DISTRIBUTIONS,
      USER_PARAMS,
      DATASET
    }
  }
}
