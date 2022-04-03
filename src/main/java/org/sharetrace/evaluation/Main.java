package org.sharetrace.evaluation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import java.time.Duration;
import java.time.Instant;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.message.NodeMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskPropagationMessage;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.util.IntervalCache;

public class Main {

  public static void main(String[] args) {
    ActorSystem.create(Behaviors.setup(Main::run), "Runner");
  }

  private static Behavior<Void> run(ActorContext<Void> context) {
    Behavior<RiskPropagationMessage> riskPropagation =
        RiskPropagationBuilder.create()
            .graph(newGraph())
            .parameters(parameters())
            .clock(Main::time)
            .scoreFactory(Main::initialScore)
            .timeFactory(Main::contactTime)
            .cacheFactory(Main::nodeCache)
            .build();
    return new Runner(riskPropagation).run();
  }

  private static ContactGraph newGraph() {
    return ContactGraph.create(new BarabasiAlbertGraphGenerator<>(2, 1, 100));
  }

  private static RiskScore initialScore(ActorRef<NodeMessage> node) {
    return RiskScore.builder().replyTo(node).value(Math.random()).timestamp(timestamp()).build();
  }

  private static Instant contactTime(ActorRef<NodeMessage> node1, ActorRef<NodeMessage> node2) {
    return timestamp();
  }

  private static Instant timestamp() {
    return Instant.now().minus(lookBack());
  }

  private static Duration lookBack() {
    return Duration.ofDays(Math.round(Math.random() * 13));
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
        .build();
  }

  private static IntervalCache<RiskScore> nodeCache() {
    return IntervalCache.<RiskScore>builder()
        .nIntervals(14)
        .interval(Duration.ofDays(1L))
        .refreshRate(Duration.ofHours(1L))
        .clock(Main::time)
        .mergeStrategy(Main::merge)
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
