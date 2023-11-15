package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Range;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import sharetrace.util.Ranges;

public final class Timestamp implements Temporal, Comparable<Timestamp> {

  private static final Range<Long> RANGE = Range.closed(0L, Long.MAX_VALUE);

  public static final Timestamp MIN = ofEpochMillis(RANGE.lowerEndpoint());
  public static final Timestamp MAX = ofEpochMillis(RANGE.upperEndpoint());

  @JsonValue private final Instant instant;

  private Timestamp(long epochMillis) {
    this.instant = Instant.ofEpochMilli(Ranges.check("epochMillis", epochMillis, RANGE));
  }

  public static Timestamp ofEpochMillis(long epochMillis) {
    return new Timestamp(epochMillis);
  }

  public static Timestamp ofEpochSeconds(long epochSeconds) {
    return from(Instant.ofEpochSecond(epochSeconds));
  }

  public static Timestamp from(Instant instant) {
    return new Timestamp(instant.toEpochMilli());
  }

  public static Timestamp parse(String string) {
    return from(Instant.parse(string));
  }

  public static Timestamp min(Timestamp left, Timestamp right) {
    return left.isBefore(right) ? left : right;
  }

  public static Timestamp max(Timestamp left, Timestamp right) {
    return left.isAfter(right) ? left : right;
  }

  @Override
  public boolean isSupported(TemporalUnit unit) {
    return instant.isSupported(unit);
  }

  @Override
  public Timestamp with(TemporalField field, long newValue) {
    return from(instant.with(field, newValue));
  }

  @Override
  public Timestamp plus(long amountToAdd, TemporalUnit unit) {
    return from(instant.plus(amountToAdd, unit));
  }

  @Override
  public Timestamp plus(TemporalAmount amount) {
    return from(instant.plus(amount));
  }

  @Override
  public Timestamp minus(TemporalAmount amount) {
    return from(instant.minus(amount));
  }

  @Override
  public Timestamp minus(long amountToSubtract, TemporalUnit unit) {
    return from(instant.minus(amountToSubtract, unit));
  }

  @Override
  public long until(Temporal endExclusive, TemporalUnit unit) {
    return instant.until(endExclusive, unit);
  }

  @Override
  public boolean isSupported(TemporalField field) {
    return instant.isSupported(field);
  }

  @Override
  public long getLong(TemporalField field) {
    return instant.getLong(field);
  }

  @Override
  public String toString() {
    return instant.toString();
  }

  @Override
  public int hashCode() {
    return instant.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Timestamp timestamp && instant.equals(timestamp.instant);
  }

  @Override
  public int compareTo(Timestamp timestamp) {
    return instant.compareTo(timestamp.instant);
  }

  public boolean isAfter(Timestamp timestamp) {
    return instant.isAfter(timestamp.instant);
  }

  public boolean isBefore(Timestamp timestamp) {
    return instant.isBefore(timestamp.instant);
  }

  public long toEpochMillis() {
    return instant.toEpochMilli();
  }
}
