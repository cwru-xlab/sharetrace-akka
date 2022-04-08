package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskScoreMessage;
import org.sharetrace.util.IntervalCache;

public abstract class AbstractExperiment<T> implements Experiment {

  protected static final Duration DEFAULT_TTL = Duration.ofDays(14);
  protected static final long DEFAULT_SEED = 12345L;

  @Override
  public void run() {
    Parameters parameters = parameters();
    Dataset<T> dataset = newDataset(parameters);
    Behavior<AlgorithmMessage> algorithm = newAlgorithm(dataset, parameters);
    Runner.run(algorithm);
  }

  protected Supplier<Instant> clock() {
    return Instant::now;
  }

  protected abstract Dataset<T> newDataset(Parameters parameters);

  protected abstract Behavior<AlgorithmMessage> newAlgorithm(
      Dataset<T> dataset, Parameters parameters);

  protected Parameters parameters() {
    return Parameters.builder()
        .sendTolerance(0.6)
        .transmissionRate(0.8)
        .timeBuffer(Duration.ofDays(2))
        .scoreTtl(DEFAULT_TTL)
        .contactTtl(DEFAULT_TTL)
        .build();
  }

  protected Supplier<IntervalCache<RiskScoreMessage>> cacheFactory() {
    return () ->
        IntervalCache.<RiskScoreMessage>builder()
            .nIntervals(DEFAULT_TTL.toDays() + 1)
            .nBuffer(1L)
            .interval(Duration.ofDays(1L))
            .refreshRate(Duration.ofHours(1L))
            .clock(clock())
            .mergeStrategy(this::mergeStrategy)
            .prioritizeReads(false)
            .build();
  }

  protected RiskScoreMessage mergeStrategy(RiskScoreMessage oldScore, RiskScoreMessage newScore) {
    double oldValue = oldScore.score().value();
    double newValue = newScore.score().value();
    Instant oldTimestamp = oldScore.score().timestamp();
    Instant newTimestamp = newScore.score().timestamp();
    boolean isHigher = oldValue < newValue;
    boolean isOlder = oldValue == newValue && oldTimestamp.isAfter(newTimestamp);
    return isHigher || isOlder ? newScore : oldScore;
  }
}
