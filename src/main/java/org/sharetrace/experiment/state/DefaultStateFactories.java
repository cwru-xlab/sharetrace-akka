package org.sharetrace.experiment.state;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDataset;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.data.factory.GraphGeneratorBuilder;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.experiment.ExperimentContext;
import org.sharetrace.experiment.GraphType;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class DefaultStateFactories {

  private static final String WHITESPACE_DELIMITER = "\\s+";

  private DefaultStateFactories() {}

  @Builder.Factory()
  public static ExperimentState defaultFileState(
      GraphType graphType, Optional<String> delimiter, Path path) {
    return ExperimentState.builder(ExperimentContext.create())
        .graphType(graphType)
        .dataset(context -> fileDataset(context, delimiter.orElse(WHITESPACE_DELIMITER), path))
        .build();
  }

  @Builder.Factory
  public static Dataset fileDataset(DatasetContext context, String delimiter, Path path) {
    return FileDataset.builder()
        .delimiter(delimiter)
        .path(path)
        .addAllLoggable(context.loggable())
        .referenceTime(context.referenceTime())
        .riskScoreFactory(context.riskScoreFactory())
        .build();
  }

  @Builder.Factory
  public static ExperimentState defaultSampledState(
      GraphType graphType, int numNodes, Optional<GraphGeneratorFactory> graphGeneratorFactory) {
    return ExperimentState.builder(ExperimentContext.create())
        .graphType(graphType)
        .dataset(context -> sampledDataset(context, numNodes, graphGeneratorFactory))
        .build();
  }

  @Builder.Factory
  public static Dataset sampledDataset(
      DatasetContext context, int numNodes, Optional<GraphGeneratorFactory> generatorFactory) {
    Supplier<GraphGeneratorFactory> defaultFactory =
        defaultFactory(context.graphType(), context.seed());
    return SampledDataset.builder()
        .addAllLoggable(context.loggable())
        .riskScoreFactory(context.riskScoreFactory())
        .contactTimeFactory(context.contactTimeFactory())
        .graphGeneratorFactory(generatorFactory.orElseGet(defaultFactory))
        .numNodes(numNodes)
        .build();
  }

  private static Supplier<GraphGeneratorFactory> defaultFactory(GraphType graphType, long seed) {
    return () ->
        nNodes ->
            GraphGeneratorBuilder.<Integer, DefaultEdge>create(graphType, nNodes, seed)
                .numEdges(nNodes * 2)
                .degree(4)
                .numNearestNeighbors(2)
                .numInitialNodes(2)
                .numNewEdges(2)
                .rewiringProbability(0.3)
                .build();
  }

  @Builder.Factory
  public static ExperimentState defaultParametersState(
      GraphType graphType,
      int numNodes,
      Optional<GraphGeneratorFactory> graphGeneratorFactory,
      float transmissionRate,
      float sendCoefficient) {
    return ExperimentState.builder(Defaults.parametersContext())
        .graphType(graphType)
        .messageParameters(
            Defaults.messageParameters()
                .withTransmissionRate(transmissionRate)
                .withSendCoefficient(sendCoefficient))
        .dataset(context -> sampledDataset(context, numNodes, graphGeneratorFactory))
        .build();
  }

  @Builder.Factory
  public static ExperimentState defaultRuntimeState(
      GraphType graphType, int numNodes, Optional<GraphGeneratorFactory> graphGeneratorFactory) {
    return ExperimentState.builder(Defaults.runtimeContext())
        .graphType(graphType)
        .dataset(context -> sampledDataset(context, numNodes, graphGeneratorFactory))
        .build();
  }
}
