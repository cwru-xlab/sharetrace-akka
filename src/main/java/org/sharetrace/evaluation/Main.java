package org.sharetrace.evaluation;

import akka.actor.typed.Behavior;
import java.time.Duration;
import java.time.Instant;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.SocioPatternsDatasetFactory;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.util.IntervalCache;

public class Main {

  public static void main(String[] args) {
    Dataset<Integer> dataset = SocioPatternsDatasetFactory.newDataset(Main::time, null);
    Parameters parameters = parameters();
    Behavior<AlgorithmMessage> riskPropagation =
        RiskPropagationBuilder.<Integer>create()
            .graph(dataset.graph())
            .parameters(parameters)
            .clock(Main::time)
            .scoreFactory(dataset::score)
            .timeFactory(dataset::timestamp)
            .cacheFactory(() -> nodeCache(parameters))
            .nodeTimeout(Duration.ofSeconds(5L))
            .nodeRefreshRate(Duration.ofHours(1L))
            .build();
    Runner.run(riskPropagation);
  }

  private static Instant time() {
    return Instant.now();
  }

  private static Parameters parameters() {
    return Parameters.builder()
        .sendTolerance(0.6)
        .transmissionRate(0.8)
        .timeBuffer(Duration.ofDays(2))
        .scoreTtl(Duration.ofDays(14))
        .contactTtl(Duration.ofDays(14))
        .build();
  }

  private static IntervalCache<RiskScore> nodeCache(Parameters parameters) {
    return IntervalCache.<RiskScore>builder()
        .nIntervals(parameters.scoreTtl().toDays() + 1)
        .nBuffer(1L)
        .interval(Duration.ofDays(1L))
        .refreshRate(Duration.ofHours(1L))
        .clock(Main::time)
        .mergeStrategy(Main::merge)
        .prioritizeReads(false)
        .build();
  }

  private static RiskScore merge(RiskScore oldScore, RiskScore newScore) {
    double oldValue = oldScore.value();
    double newValue = newScore.value();
    Instant oldTimestamp = oldScore.timestamp();
    Instant newTimestamp = newScore.timestamp();
    boolean isHigher = oldValue < newValue;
    boolean isOlder = oldValue == newValue && oldTimestamp.isAfter(newTimestamp);
    return isHigher || isOlder ? newScore : oldScore;
  }
}
