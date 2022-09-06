package org.sharetrace.experiment.state;

import com.google.common.math.DoubleMath;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.immutables.builder.Builder;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDataset;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.data.factory.GraphGeneratorBuilder;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.experiment.ExperimentContext;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.logging.metrics.TopologyMetric;
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.message.MessageParameters;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.CacheParameters;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class Defaults {

  private static final String WHITESPACE_DELIMITER = "\\s+";
  private static final double MIN_BASE = Math.log(1.1);
  private static final double MAX_BASE = Math.log(10d);
  private static final double DECAY_RATE = 1.75E-7;
  private static final Duration DEFAULT_TTL = Duration.ofDays(14L);
  private static final MessageParameters MESSAGE_PARAMETERS = newMessageParameters();
  private static final CacheParameters<RiskScoreMessage> CACHE_PARAMETERS = newCacheParameters();
  private static final ExperimentContext CONTEXT = ExperimentContext.create();
  private static final ExperimentContext RUNTIME_CONTEXT = newRuntimeContext();
  private static final ExperimentContext PARAMETERS_CONTEXT = newParametersContext();

  private Defaults() {}

  public static MessageParameters messageParameters() {
    return MESSAGE_PARAMETERS;
  }

  public static CacheParameters<RiskScoreMessage> cacheParameters() {
    return CACHE_PARAMETERS;
  }

  public static UserParameters userParameters(Dataset dataset) {
    return UserParameters.builder()
        .refreshPeriod(Duration.ofHours(1L))
        .idleTimeout(idleTimeout(dataset))
        .build();
  }

  public static Duration idleTimeout(Dataset dataset) {
    double nContacts = dataset.getContactNetwork().numContacts();
    double targetBase = Math.max(MIN_BASE, MAX_BASE - DECAY_RATE * nContacts);
    long timeout = (long) Math.ceil(Math.log(nContacts) / targetBase);
    return Duration.ofSeconds(timeout);
  }

  @Builder.Factory
  static ExperimentState defaultFileState(
      GraphType graphType, Optional<String> delimiter, Path path) {
    return ExperimentState.builder(CONTEXT)
        .graphType(graphType)
        .dataset(context -> fileDataset(context, delimiter.orElse(WHITESPACE_DELIMITER), path))
        .build();
  }

  @Builder.Factory
  static Dataset fileDataset(DatasetContext context, String delimiter, Path path) {
    return FileDataset.builder()
        .delimiter(delimiter)
        .path(path)
        .addAllLoggable(context.loggable())
        .referenceTime(context.referenceTime())
        .riskScoreFactory(context.riskScoreFactory())
        .build();
  }

  @Builder.Factory
  static ExperimentState defaultSampledState(
      GraphType graphType, int numNodes, Optional<GraphGeneratorFactory> graphGeneratorFactory) {
    return ExperimentState.builder(CONTEXT)
        .graphType(graphType)
        .dataset(context -> sampledDataset(context, numNodes, graphGeneratorFactory))
        .build();
  }

  @Builder.Factory
  static Dataset sampledDataset(
      DatasetContext context, int numNodes, Optional<GraphGeneratorFactory> generatorFactory) {
    return SampledDataset.builder()
        .addAllLoggable(context.loggable())
        .riskScoreFactory(context.riskScoreFactory())
        .contactTimeFactory(context.contactTimeFactory())
        .graphGeneratorFactory(generatorFactory.orElseGet(defaultFactory(context)))
        .numNodes(numNodes)
        .build();
  }

  private static Supplier<GraphGeneratorFactory> defaultFactory(DatasetContext cxt) {
    return () ->
        nNodes ->
            GraphGeneratorBuilder.<Integer, DefaultEdge>create(cxt.graphType(), nNodes, cxt.seed())
                .numEdges(nNodes * 2)
                .degree(4)
                .numNearestNeighbors(2)
                .numInitialNodes(2)
                .numNewEdges(2)
                .rewiringProbability(0.3)
                .build();
  }

  @Builder.Factory
  static ExperimentState defaultParametersState(
      GraphType graphType,
      int numNodes,
      Optional<GraphGeneratorFactory> graphGeneratorFactory,
      float transmissionRate,
      float sendCoefficient) {
    return ExperimentState.builder(PARAMETERS_CONTEXT)
        .graphType(graphType)
        .messageParameters(
            MESSAGE_PARAMETERS
                .withTransmissionRate(transmissionRate)
                .withSendCoefficient(sendCoefficient))
        .dataset(context -> sampledDataset(context, numNodes, graphGeneratorFactory))
        .build();
  }

  @Builder.Factory
  static ExperimentState defaultRuntimeState(
      GraphType graphType, int numNodes, Optional<GraphGeneratorFactory> graphGeneratorFactory) {
    return ExperimentState.builder(RUNTIME_CONTEXT)
        .graphType(graphType)
        .dataset(context -> sampledDataset(context, numNodes, graphGeneratorFactory))
        .build();
  }

  private static ExperimentContext newParametersContext() {
    return CONTEXT.withLoggable(
        CONTEXT.loggable().stream()
            .filter(Predicate.not(loggable -> loggable.equals(TopologyMetric.class)))
            .collect(Collectors.toUnmodifiableSet()));
  }

  private static ExperimentContext newRuntimeContext() {
    return CONTEXT.withLoggable(
        Set.of(SizeMetrics.class, RuntimeMetric.class, ExperimentSettings.class));
  }

  private static CacheParameters<RiskScoreMessage> newCacheParameters() {
    return CacheParameters.<RiskScoreMessage>builder()
        .interval(Duration.ofDays(1L))
        .numIntervals((int) (2 * DEFAULT_TTL.toDays()))
        .refreshPeriod(Duration.ofHours(1L))
        .mergeStrategy(Defaults::cacheMerge)
        .numLookAhead(1)
        .build();
  }

  private static RiskScoreMessage cacheMerge(RiskScoreMessage oldMsg, RiskScoreMessage newMsg) {
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

  private static boolean isApproxEqual(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return DoubleMath.fuzzyEquals(
        msg1.score().value(), msg2.score().value(), MESSAGE_PARAMETERS.scoreTolerance());
  }

  private static MessageParameters newMessageParameters() {
    return MessageParameters.builder()
        .transmissionRate(0.8f)
        .sendCoefficient(0.6f)
        .scoreTtl(DEFAULT_TTL)
        .contactTtl(DEFAULT_TTL)
        .scoreTolerance(0.01f)
        .timeBuffer(Duration.ofDays(2L))
        .build();
  }
}
