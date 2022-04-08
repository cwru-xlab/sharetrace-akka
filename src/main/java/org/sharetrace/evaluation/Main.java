package org.sharetrace.evaluation;

import akka.actor.typed.Behavior;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.SocioPatternsDatasetBuilder;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskScoreMessage;
import org.sharetrace.util.IntervalCache;

public class Main {

  public static void main(String[] args) {
    System.out.println(System.getProperty("user.dir"));
    Parameters parameters = parameters();
    Duration scoreTtl = parameters.scoreTtl();
    Dataset<Integer> dataset =
        SocioPatternsDatasetBuilder.create()
            .clock(Main::time)
            .path(Path.of("./src/main/resources/datasets/conference.txt"))
            .delimiter(" ")
            .scoreTtl(scoreTtl)
            .build();
    Behavior<AlgorithmMessage> riskPropagation =
        RiskPropagationBuilder.<Integer>create()
            .graph(dataset.graph())
            .parameters(parameters)
            .clock(Main::time)
            .scoreFactory(dataset::score)
            .timeFactory(dataset::timestamp)
            .cacheFactory(() -> nodeCache(scoreTtl))
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

  private static IntervalCache<RiskScoreMessage> nodeCache(Duration scoreTtl) {
    return IntervalCache.<RiskScoreMessage>builder()
        .nIntervals(scoreTtl.toDays() + 1)
        .nBuffer(1L)
        .interval(Duration.ofDays(1L))
        .refreshRate(Duration.ofHours(1L))
        .clock(Main::time)
        .mergeStrategy(Main::merge)
        .prioritizeReads(false)
        .build();
  }

  private static RiskScoreMessage merge(RiskScoreMessage oldScore, RiskScoreMessage newScore) {
    double oldValue = oldScore.score().value();
    double newValue = newScore.score().value();
    Instant oldTimestamp = oldScore.score().timestamp();
    Instant newTimestamp = newScore.score().timestamp();
    boolean isHigher = oldValue < newValue;
    boolean isOlder = oldValue == newValue && oldTimestamp.isAfter(newTimestamp);
    return isHigher || isOlder ? newScore : oldScore;
  }
}
