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

  public static Builder builder(ExperimentContext context) {
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
    // Ordered by dependency
    GRAPH_TYPE,
    ID,
    MDC,
    MESSAGE_PARAMETERS,
    CACHE_PARAMETERS,
    RISK_SCORE_VALUE,
    RISK_SCORE_TIME,
    CONTACT_TIME,
    PDFS,
    FACTORIES,
    DATASET,
    USER_PARAMETERS
  }

  public static class Builder
      implements GraphTypeContext,
          IdContext,
          MdcContext,
          MessageParametersContext,
          CacheParametersContext,
          PdfFactoryContext,
          DatasetContext,
          UserParametersContext {

    private final ExperimentContext context;
    private final Loggables loggables;
    private final Setters setters;
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
      setters = new Setters();
      mdc = new HashMap<>();
      id(context -> newId());
      mdc(context -> Logging.mdc(context.id(), context.graphType()));
      messageParameters(context -> Defaults.messageParameters());
      cacheParameters(context -> Defaults.cacheParameters());
      riskScoreTimePdfFactory(defaultPdfFactory());
      riskScoreValuePdfFactory(defaultPdfFactory());
      contactTimePdfFactory(defaultPdfFactory());
      userParameters(context -> Defaults.userParameters(context.dataset()));
    }

    private Function<PdfFactoryContext, PdfFactory> defaultPdfFactory() {
      return context -> seed -> new UniformRealDistribution(new Well512a(seed), 0d, 1d);
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

    public Builder mdc(Function<MdcContext, Map<String, String>> factory) {
      setters.put(Setter.MDC, factory.andThen(this::mdc));
      return this;
    }

    public Builder mdc(Map<String, String> mdc) {
      this.mdc.putAll(Objects.requireNonNull(mdc));
      setters.remove(Setter.MDC);
      return this;
    }

    public Builder messageParameters(
        Function<MessageParametersContext, MessageParameters> factory) {
      setters.put(Setter.MESSAGE_PARAMETERS, factory.andThen(this::messageParameters));
      return this;
    }

    public Builder messageParameters(MessageParameters parameters) {
      messageParameters = Objects.requireNonNull(parameters);
      setters.remove(Setter.MESSAGE_PARAMETERS);
      return this;
    }

    public Builder cacheParameters(
        Function<CacheParametersContext, CacheParameters<RiskScoreMessage>> factory) {
      setters.put(Setter.CACHE_PARAMETERS, factory.andThen(this::cacheParameters));
      return this;
    }

    public Builder cacheParameters(CacheParameters<RiskScoreMessage> parameters) {
      cacheParameters = Objects.requireNonNull(parameters);
      setters.remove(Setter.CACHE_PARAMETERS);
      return this;
    }

    public Builder riskScoreTimePdfFactory(Function<PdfFactoryContext, PdfFactory> factory) {
      setters.put(Setter.RISK_SCORE_TIME, factory.andThen(this::riskScoreTimePdfFactory));
      return this;
    }

    public Builder riskScoreTimePdfFactory(PdfFactory pdfFactory) {
      riskScoreTimePdfFactory = Objects.requireNonNull(pdfFactory);
      setters.remove(Setter.RISK_SCORE_TIME);
      return this;
    }

    public Builder riskScoreValuePdfFactory(Function<PdfFactoryContext, PdfFactory> factory) {
      setters.put(Setter.RISK_SCORE_VALUE, factory.andThen(this::riskScoreValuePdfFactory));
      return this;
    }

    public Builder riskScoreValuePdfFactory(PdfFactory pdfFactory) {
      riskScoreValuePdfFactory = Objects.requireNonNull(pdfFactory);
      setters.remove(Setter.RISK_SCORE_VALUE);
      return this;
    }

    public Builder contactTimePdfFactory(Function<PdfFactoryContext, PdfFactory> factory) {
      setters.put(Setter.CONTACT_TIME, factory.andThen(this::contactTimePdfFactory));
      return this;
    }

    public Builder contactTimePdfFactory(PdfFactory pdfFactory) {
      contactTimePdfFactory = Objects.requireNonNull(pdfFactory);
      setters.remove(Setter.CONTACT_TIME);
      return this;
    }

    public Builder userParameters(Function<UserParametersContext, UserParameters> factory) {
      setters.put(Setter.USER_PARAMETERS, factory.andThen(this::userParameters));
      return this;
    }

    public Builder userParameters(UserParameters parameters) {
      userParameters = Objects.requireNonNull(parameters);
      setters.remove(Setter.USER_PARAMETERS);
      return this;
    }

    private Builder(ExperimentState state) {
      context = state.context;
      loggables = state.loggables;
      setters = new Setters();
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

    public ExperimentState build() {
      setters.put(Setter.PDFS, x -> setPdfs());
      setters.put(Setter.FACTORIES, x -> setFactories());
      setters.values().forEach(setter -> setter.apply(this));
      return new ExperimentState(this);
    }

    private Builder setPdfs() {
      long seed = context.seed();
      riskScoreValuePdf = riskScoreValuePdfFactory.getPdf(seed);
      riskScoreTimePdf = riskScoreTimePdfFactory.getPdf(seed);
      contactTimePdf = contactTimePdfFactory.getPdf(seed);
      return this;
    }

    private Builder setFactories() {
      Sampler<RiskScore> riskScoreSampler = newRiskScoreSampler();
      riskScoreFactory = RiskScoreFactory.fromSupplier(riskScoreSampler::sample);
      Sampler<Instant> contactTimeSampler = newContactTimeSampler();
      contactTimeFactory = ContactTimeFactory.fromSupplier(contactTimeSampler::sample);
      return this;
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
      return Collections.unmodifiableMap(mdc);
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

    public Builder graphType(Function<GraphTypeContext, GraphType> factory) {
      setters.put(Setter.GRAPH_TYPE, factory.andThen(this::graphType));
      return this;
    }

    public Builder graphType(GraphType graphType) {
      this.graphType = Objects.requireNonNull(graphType);
      setters.remove(Setter.GRAPH_TYPE);
      return this;
    }

    public Builder dataset(Function<DatasetContext, Dataset> factory) {
      setters.put(Setter.DATASET, factory.andThen(this::dataset));
      return this;
    }

    public Builder dataset(Dataset dataset) {
      this.dataset = Objects.requireNonNull(dataset);
      setters.remove(Setter.DATASET);
      return this;
    }
  }

  private static final class Setters extends EnumMap<Setter, Function<? super Builder, ?>> {

    public Setters() {
      super(Setter.class);
    }
  }
}
