package org.sharetrace.experiment.state;

import akka.actor.typed.Behavior;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final Map<Class<? super Builder>, Function<? super Builder, ?>> preDatasetBuilders;
    private final Map<Class<? super Builder>, Function<? super Builder, ?>> postDatasetBuilders;
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

    private Builder(ExperimentContext baseContext) {
      context = baseContext;
      loggables = Loggables.create(baseContext.loggable(), logger);
      preDatasetBuilders = newBuildersMap();
      postDatasetBuilders = newBuildersMap();
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

    private static Map<Class<? super Builder>, Function<? super Builder, ?>> newBuildersMap() {
      return new LinkedHashMap<>();
    }

    private Function<PdfFactoryContext, PdfFactory> defaultPdfFactory() {
      return context -> seed -> new UniformRealDistribution(new Well512a(seed), 0d, 1d);
    }

    private static String newId() {
      return String.valueOf(new Random().nextLong());
    }

    private Builder(ExperimentState state) {
      context = state.context;
      loggables = state.loggables;
      preDatasetBuilders = newBuildersMap();
      postDatasetBuilders = newBuildersMap();
      mdc = state.mdc;
      graphType = state.graphType;
      id(context -> newId());
      messageParameters = state.messageParameters;
      cacheParameters = state.cacheParameters;
      riskScoreValuePdfFactory = state.riskScoreValuePdfFactory;
      riskScoreTimePdfFactory = state.riskScoreTimePdfFactory;
      contactTimePdfFactory = state.contactTimePdfFactory;
      dataset = state.dataset;
      userParameters = state.userParameters;
    }

    @Override
    public ExperimentState build() {
      applyBuilders(preDatasetBuilders);
      setPdfs();
      setFactories();
      applyBuilders(postDatasetBuilders);
      return new ExperimentState(this);
    }

    private void applyBuilders(Map<Class<? super Builder>, Function<? super Builder, ?>> builders) {
      builders.values().forEach(setter -> setter.apply(this));
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
      preDatasetBuilders.remove(IdBuilder.class);
      return this;
    }

    @Override
    public MdcBuilder id(Function<IdContext, String> factory) {
      preDatasetBuilders.put(IdBuilder.class, factory.andThen(this::id));
      return this;
    }

    @Override
    public MessageParametersBuilder mdc(Map<String, String> mdc) {
      this.mdc.putAll(Objects.requireNonNull(mdc));
      preDatasetBuilders.remove(MdcBuilder.class);
      return this;
    }

    @Override
    public MessageParametersBuilder mdc(Function<MdcContext, Map<String, String>> factory) {
      preDatasetBuilders.put(MdcBuilder.class, factory.andThen(this::mdc));
      return this;
    }

    @Override
    public CacheParametersBuilder messageParameters(MessageParameters parameters) {
      messageParameters = Objects.requireNonNull(parameters);
      preDatasetBuilders.remove(MessageParametersBuilder.class);
      return this;
    }

    @Override
    public CacheParametersBuilder messageParameters(
        Function<MessageParametersContext, MessageParameters> factory) {
      preDatasetBuilders.put(
          MessageParametersBuilder.class, factory.andThen(this::messageParameters));
      return this;
    }

    @Override
    public RiskScoreValueBuilder cacheParameters(CacheParameters<RiskScoreMessage> parameters) {
      cacheParameters = Objects.requireNonNull(parameters);
      preDatasetBuilders.remove(CacheParametersBuilder.class);
      return this;
    }

    @Override
    public RiskScoreValueBuilder cacheParameters(
        Function<CacheParametersContext, CacheParameters<RiskScoreMessage>> factory) {
      preDatasetBuilders.put(CacheParametersBuilder.class, factory.andThen(this::cacheParameters));
      return this;
    }

    @Override
    public ContactTimeBuilder riskScoreTimePdfFactory(PdfFactory pdfFactory) {
      riskScoreTimePdfFactory = Objects.requireNonNull(pdfFactory);
      preDatasetBuilders.remove(RiskScoreTimeBuilder.class);
      return this;
    }

    @Override
    public ContactTimeBuilder riskScoreTimePdfFactory(
        Function<PdfFactoryContext, PdfFactory> factory) {
      preDatasetBuilders.put(
          RiskScoreTimeBuilder.class, factory.andThen(this::riskScoreTimePdfFactory));
      return this;
    }

    @Override
    public RiskScoreTimeBuilder riskScoreValuePdfFactory(PdfFactory pdfFactory) {
      riskScoreValuePdfFactory = Objects.requireNonNull(pdfFactory);
      preDatasetBuilders.remove(RiskScoreValueBuilder.class);
      return this;
    }

    @Override
    public RiskScoreTimeBuilder riskScoreValuePdfFactory(
        Function<PdfFactoryContext, PdfFactory> factory) {
      preDatasetBuilders.put(
          RiskScoreValueBuilder.class, factory.andThen(this::riskScoreValuePdfFactory));
      return this;
    }

    @Override
    public DatasetBuilder contactTimePdfFactory(PdfFactory pdfFactory) {
      contactTimePdfFactory = Objects.requireNonNull(pdfFactory);
      preDatasetBuilders.remove(ContactTimeBuilder.class);
      return this;
    }

    @Override
    public DatasetBuilder contactTimePdfFactory(Function<PdfFactoryContext, PdfFactory> factory) {
      preDatasetBuilders.put(
          ContactTimeBuilder.class, factory.andThen(this::contactTimePdfFactory));
      return this;
    }

    @Override
    public FinalBuilder userParameters(UserParameters parameters) {
      userParameters = Objects.requireNonNull(parameters);
      postDatasetBuilders.remove(UserParametersBuilder.class);
      return this;
    }

    @Override
    public FinalBuilder userParameters(Function<UserParametersContext, UserParameters> factory) {
      postDatasetBuilders.put(UserParametersBuilder.class, factory.andThen(this::userParameters));
      return this;
    }

    @Override
    public IdBuilder graphType(GraphType graphType) {
      this.graphType = Objects.requireNonNull(graphType);
      preDatasetBuilders.remove(GraphTypeBuilder.class);
      return this;
    }

    @Override
    public IdBuilder graphType(Function<GraphTypeContext, GraphType> factory) {
      preDatasetBuilders.put(GraphTypeBuilder.class, factory.andThen(this::graphType));
      return this;
    }

    @Override
    public UserParametersBuilder dataset(Dataset dataset) {
      this.dataset = Objects.requireNonNull(dataset);
      postDatasetBuilders.remove(DatasetBuilder.class);
      return this;
    }

    @Override
    public UserParametersBuilder dataset(Function<DatasetContext, Dataset> factory) {
      postDatasetBuilders.put(DatasetBuilder.class, factory.andThen(this::dataset));
      return this;
    }
  }
}
