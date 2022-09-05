package org.sharetrace.experiment.state;

import akka.actor.typed.Behavior;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import org.sharetrace.data.factory.PdfFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.data.sampling.RiskScoreSampler;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.TimeSampler;
import org.sharetrace.experiment.ExperimentContext;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.logging.settings.LoggableSetting;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.MessageParameters;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.CacheParameters;
import org.sharetrace.util.IntervalCache;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class ExperimentState {

  private static final Logger logger = Logging.settingLogger();
  private final ExperimentContext context;
  private final Loggables loggables;
  private final String id;
  private final GraphType graphType;
  private final Map<String, String> mdc;
  private final PdfFactory riskScoreValuePdfFactory;
  private final PdfFactory riskScoreTimePdfFactory;
  private final PdfFactory contactTimePdfFactory;
  private final Dataset dataset;
  private final UserParameters userParameters;
  private final MessageParameters messageParameters;
  private final CacheParameters<RiskScoreMessage> cacheParameters;

  private ExperimentState(Builder builder) {
    context = builder.context;
    loggables = builder.loggables;
    graphType = builder.graphType;
    id = builder.id;
    mdc = builder.mdc;
    riskScoreValuePdfFactory = builder.riskScoreValuePdfFactory;
    riskScoreTimePdfFactory = builder.riskScoreTimePdfFactory;
    contactTimePdfFactory = builder.contactTimePdfFactory;
    dataset = builder.dataset;
    userParameters = builder.userParameters;
    messageParameters = builder.messageParameters;
    cacheParameters = builder.cacheParameters;
  }

  public static GraphTypeBuilder builder(ExperimentContext context) {
    return new Builder(context);
  }

  public void run() {
    setup();
    runAlgorithm();
  }

  private void setup() {
    mdc.forEach(MDC::put);
    dataset.getContactNetwork().logMetrics();
    loggables.log(LoggableSetting.KEY, settings());
  }

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .graphType(graphType.toString())
        .stateId(id)
        .seed(context.seed())
        .userParameters(userParameters)
        .messageParameters(messageParameters)
        .cacheParameters(cacheParameters)
        .build();
  }

  private void runAlgorithm() {
    Runner.run(newAlgorithm(), "RiskPropagation");
  }

  private Behavior<AlgorithmMessage> newAlgorithm() {
    return RiskPropagationBuilder.create()
        .addAllLoggable(loggables.loggable())
        .putAllMdc(mdc)
        .contactNetwork(dataset.getContactNetwork())
        .userParameters(userParameters)
        .messageParameters(messageParameters)
        .clock(context.clock())
        .riskScoreFactory(dataset)
        .cacheFactory(cacheFactory())
        .build();
  }

  protected CacheFactory<RiskScoreMessage> cacheFactory() {
    return () ->
        IntervalCache.<RiskScoreMessage>builder()
            .clock(context.clock())
            .nIntervals(cacheParameters.numIntervals())
            .nLookAhead(cacheParameters.numLookAhead())
            .interval(cacheParameters.interval())
            .refreshPeriod(cacheParameters.refreshPeriod())
            .mergeStrategy(cacheParameters.mergeStrategy())
            .comparator(RiskScoreMessage::compareTo)
            .build();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public Dataset dataset() {
    return dataset;
  }

  public MessageParameters messageParameters() {
    return messageParameters;
  }

  private enum Setter {
    GRAPH_TYPE,
    ID,
    MDC,
    MESSAGE_PARAMETERS,
    CACHE_PARAMETERS,
    SCORE_VALUE,
    SCORE_TIME,
    CONTACT_TIME,
    DATASET,
    USER_PARAMETERS
  }

  public static class Builder
      implements GraphTypeBuilder,
          IdBuilder,
          MdcBuilder,
          MessageParametersBuilder,
          CacheParametersBuilder,
          RiskScoreValueBuilder,
          RiskScoreTimeBuilder,
          ContactTimeBuilder,
          DatasetBuilder,
          UserParametersBuilder,
          FinalBuilder,
          GraphTypeContext,
          IdContext,
          MdcContext,
          MessageParametersContext,
          CacheParametersContext,
          PdfFactoryContext,
          DatasetContext,
          UserParametersContext {

    private final ExperimentContext context;
    private final Loggables loggables;
    private final Map<Setter, Function<? super Builder, ?>> preDatasetSetters;
    private final Map<Setter, Function<? super Builder, ?>> postDatasetSetters;
    private final Map<String, String> mdc;
    private GraphType graphType;
    private String id;
    private MessageParameters messageParameters;
    private CacheParameters<RiskScoreMessage> cacheParameters;
    private PdfFactory riskScoreValuePdfFactory;
    private PdfFactory riskScoreTimePdfFactory;
    private PdfFactory contactTimePdfFactory;
    private RealDistribution riskScoreValuePdf;
    private RealDistribution riskScoreTimePdf;
    private RealDistribution contactTimePdf;
    private RiskScoreFactory riskScoreFactory;
    private ContactTimeFactory contactTimeFactory;
    private Dataset dataset;
    private UserParameters userParameters;

    private Builder(ExperimentContext context) {
      this.context = context;
      loggables = Loggables.create(context.loggable(), logger);
      preDatasetSetters = newSettersMap();
      postDatasetSetters = newSettersMap();
      mdc = new HashMap<>();
      id(cxt -> newId());
      mdc(cxt -> Logging.mdc(cxt.id(), cxt.graphType()));
      messageParameters(cxt -> Defaults.messageParameters());
      cacheParameters(cxt -> Defaults.cacheParameters());
      riskScoreTimePdfFactory(defaultPdfFactory());
      riskScoreValuePdfFactory(defaultPdfFactory());
      contactTimePdfFactory(defaultPdfFactory());
      userParameters(cxt -> Defaults.userParameters(cxt.dataset()));
    }

    private static Map<Setter, Function<? super Builder, ?>> newSettersMap() {
      return new EnumMap<>(Setter.class);
    }

    private Function<PdfFactoryContext, PdfFactory> defaultPdfFactory() {
      return cxt -> seed -> new UniformRealDistribution(new Well512a(seed), 0d, 1d);
    }

    private static String newId() {
      return String.valueOf(new Random().nextLong());
    }

    private Builder(ExperimentState iteration) {
      context = iteration.context;
      loggables = iteration.loggables;
      preDatasetSetters = newSettersMap();
      postDatasetSetters = newSettersMap();
      mdc = iteration.mdc;
      graphType = iteration.graphType;
      id(cxt -> newId());
      messageParameters = iteration.messageParameters;
      cacheParameters = iteration.cacheParameters;
      riskScoreValuePdfFactory = iteration.riskScoreValuePdfFactory;
      riskScoreTimePdfFactory = iteration.riskScoreTimePdfFactory;
      contactTimePdfFactory = iteration.contactTimePdfFactory;
      dataset = iteration.dataset;
      userParameters = iteration.userParameters;
    }

    @Override
    public ExperimentState build() {
      applySetters(preDatasetSetters);
      setPdfs();
      setFactories();
      applySetters(postDatasetSetters);
      return new ExperimentState(this);
    }

    private void applySetters(Map<Setter, Function<? super Builder, ?>> setters) {
      setters.values().forEach(setter -> setter.apply(this));
    }

    private void setPdfs() {
      long seed = context.seed();
      riskScoreValuePdf = riskScoreValuePdfFactory.getPdf(seed);
      riskScoreTimePdf = riskScoreTimePdfFactory.getPdf(seed);
      contactTimePdf = contactTimePdfFactory.getPdf(seed);
    }

    private void setFactories() {
      Sampler<RiskScore> riskScoreSampler = newRiskScoreSampler();
      riskScoreFactory = RiskScoreFactory.fromSupplier(riskScoreSampler::sample);
      Sampler<Instant> contactTimeSampler = newContactTimeSampler();
      contactTimeFactory = ContactTimeFactory.fromSupplier(contactTimeSampler::sample);
    }

    private Sampler<RiskScore> newRiskScoreSampler() {
      return RiskScoreSampler.builder()
          .valueDistribution(riskScoreValuePdf)
          .timeSampler(newTimeSampler(riskScoreTimePdf, messageParameters.scoreTtl()))
          .build();
    }

    private Sampler<Instant> newTimeSampler(RealDistribution ttlDistribution, Duration ttl) {
      return TimeSampler.builder()
          .ttlDistribution(ttlDistribution)
          .ttl(ttl)
          .referenceTime(context.referenceTime())
          .build();
    }

    private Sampler<Instant> newContactTimeSampler() {
      return newTimeSampler(contactTimePdf, messageParameters.contactTtl());
    }

    @Override
    public Instant referenceTime() {
      return context.referenceTime();
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
      return loggables.loggable();
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
      return mdc;
    }

    @Override
    public MessageParameters messageParameters() {
      return messageParameters;
    }

    @Override
    public CacheParameters<RiskScoreMessage> cacheParameters() {
      return cacheParameters;
    }

    @Override
    public RiskScoreFactory riskScoreFactory() {
      return riskScoreFactory;
    }

    @Override
    public ContactTimeFactory contactTimeFactory() {
      return contactTimeFactory;
    }

    @Override
    public Dataset dataset() {
      return dataset;
    }

    @Override
    public MdcBuilder id(String id) {
      this.id = Objects.requireNonNull(id);
      preDatasetSetters.remove(Setter.ID);
      return this;
    }

    @Override
    public MdcBuilder id(Function<IdContext, String> factory) {
      preDatasetSetters.put(Setter.ID, factory.andThen(this::id));
      return this;
    }

    @Override
    public MessageParametersBuilder mdc(Map<String, String> mdc) {
      this.mdc.putAll(Objects.requireNonNull(mdc));
      preDatasetSetters.remove(Setter.MDC);
      return this;
    }

    @Override
    public MessageParametersBuilder mdc(Function<MdcContext, Map<String, String>> factory) {
      preDatasetSetters.put(Setter.MDC, factory.andThen(this::mdc));
      return this;
    }

    @Override
    public CacheParametersBuilder messageParameters(MessageParameters parameters) {
      messageParameters = Objects.requireNonNull(parameters);
      preDatasetSetters.remove(Setter.MESSAGE_PARAMETERS);
      return this;
    }

    @Override
    public CacheParametersBuilder messageParameters(
        Function<MessageParametersContext, MessageParameters> factory) {
      preDatasetSetters.put(Setter.MESSAGE_PARAMETERS, factory.andThen(this::messageParameters));
      return this;
    }

    @Override
    public RiskScoreValueBuilder cacheParameters(CacheParameters<RiskScoreMessage> parameters) {
      cacheParameters = Objects.requireNonNull(parameters);
      preDatasetSetters.remove(Setter.CACHE_PARAMETERS);
      return this;
    }

    @Override
    public RiskScoreValueBuilder cacheParameters(
        Function<CacheParametersContext, CacheParameters<RiskScoreMessage>> factory) {
      preDatasetSetters.put(Setter.CACHE_PARAMETERS, factory.andThen(this::cacheParameters));
      return this;
    }

    @Override
    public ContactTimeBuilder riskScoreTimePdfFactory(PdfFactory pdfFactory) {
      riskScoreTimePdfFactory = Objects.requireNonNull(pdfFactory);
      preDatasetSetters.remove(Setter.SCORE_TIME);
      return this;
    }

    @Override
    public ContactTimeBuilder riskScoreTimePdfFactory(
        Function<PdfFactoryContext, PdfFactory> factory) {
      preDatasetSetters.put(Setter.SCORE_TIME, factory.andThen(this::riskScoreTimePdfFactory));
      return this;
    }

    @Override
    public RiskScoreTimeBuilder riskScoreValuePdfFactory(PdfFactory pdfFactory) {
      riskScoreValuePdfFactory = Objects.requireNonNull(pdfFactory);
      preDatasetSetters.remove(Setter.SCORE_VALUE);
      return this;
    }

    @Override
    public RiskScoreTimeBuilder riskScoreValuePdfFactory(
        Function<PdfFactoryContext, PdfFactory> factory) {
      preDatasetSetters.put(Setter.SCORE_VALUE, factory.andThen(this::riskScoreValuePdfFactory));
      return this;
    }

    @Override
    public DatasetBuilder contactTimePdfFactory(PdfFactory pdfFactory) {
      contactTimePdfFactory = Objects.requireNonNull(pdfFactory);
      preDatasetSetters.remove(Setter.CONTACT_TIME);
      return this;
    }

    @Override
    public DatasetBuilder contactTimePdfFactory(Function<PdfFactoryContext, PdfFactory> factory) {
      preDatasetSetters.put(Setter.CONTACT_TIME, factory.andThen(this::contactTimePdfFactory));
      return this;
    }

    @Override
    public FinalBuilder userParameters(UserParameters parameters) {
      userParameters = Objects.requireNonNull(parameters);
      postDatasetSetters.remove(Setter.USER_PARAMETERS);
      return this;
    }

    @Override
    public FinalBuilder userParameters(Function<UserParametersContext, UserParameters> factory) {
      postDatasetSetters.put(Setter.USER_PARAMETERS, factory.andThen(this::userParameters));
      return this;
    }

    @Override
    public IdBuilder graphType(GraphType graphType) {
      this.graphType = Objects.requireNonNull(graphType);
      preDatasetSetters.remove(Setter.GRAPH_TYPE);
      return this;
    }

    @Override
    public IdBuilder graphType(Function<GraphTypeContext, GraphType> factory) {
      preDatasetSetters.put(Setter.GRAPH_TYPE, factory.andThen(this::graphType));
      return this;
    }

    @Override
    public UserParametersBuilder dataset(Dataset dataset) {
      this.dataset = Objects.requireNonNull(dataset);
      postDatasetSetters.remove(Setter.DATASET);
      return this;
    }

    @Override
    public UserParametersBuilder dataset(Function<DatasetContext, Dataset> factory) {
      postDatasetSetters.put(Setter.DATASET, factory.andThen(this::dataset));
      return this;
    }
  }
}
