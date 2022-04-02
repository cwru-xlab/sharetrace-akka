package org.sharetrace.propagation.model;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import java.util.UUID;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RiskScore implements NodeMessage, Comparable<RiskScore> {

  public static final double MIN_VALUE = 0d;
  public static final double MAX_VALUE = 1d;
  private static final String VALUE_MESSAGE =
      "'value' must be between " + MIN_VALUE + " and " + MAX_VALUE + ", inclusive; got %s";

  public static Builder builder() {
    return ImmutableRiskScore.builder();
  }

  public abstract ActorRef<NodeMessage> replyTo();

  public abstract double value();

  public abstract Instant timestamp();

  @Value.Default
  public String uuid() {
    return UUID.randomUUID().toString();
  }

  @Override
  public int compareTo(RiskScore score) {
    int byValue = Double.compare(value(), score.value());
    return byValue != 0 ? byValue : timestamp().compareTo(score.timestamp());
  }

  @Value.Check
  protected void check() {
    double value = value();
    if (value < MIN_VALUE || value > MAX_VALUE) {
      throw new IllegalArgumentException(String.format(VALUE_MESSAGE, value));
    }
  }

  public interface Builder {

    Builder replyTo(ActorRef<NodeMessage> replyTo);

    Builder value(double value);

    Builder timestamp(Instant timestamp);

    Builder uuid(String uuid);

    RiskScore build();
  }
}
