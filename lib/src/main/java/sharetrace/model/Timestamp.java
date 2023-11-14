package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Range;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import javax.annotation.concurrent.Immutable;
import sharetrace.util.Ranges;

@Immutable
@SuppressWarnings("deprecation")
public final class Timestamp extends Date implements Temporal {

  private static final Range<Long> RANGE = Range.closed(0L, Long.MAX_VALUE);

  public static final Timestamp MIN = ofEpochMillis(RANGE.lowerEndpoint());
  public static final Timestamp MAX = ofEpochMillis(RANGE.upperEndpoint());

  @JsonValue private final Instant instant;

  private Timestamp(long epochMillis) {
    super(Ranges.check("epochMillis", epochMillis, RANGE));
    this.instant = Instant.ofEpochMilli(epochMillis);
  }

  private Timestamp(int year, int month, int date, Instant instant) {
    throw new UnsupportedOperationException();
  }

  private Timestamp(int year, int month, int date, int hrs, int min, Instant instant) {
    throw new UnsupportedOperationException();
  }

  private Timestamp(int year, int month, int date, int hrs, int min, int sec, Instant instant) {
    throw new UnsupportedOperationException();
  }

  private Timestamp(String s, Instant instant) {
    throw new UnsupportedOperationException();
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

  public static Timestamp parseIso8601(String string) {
    return from(Instant.parse(string));
  }

  public static Timestamp min(Timestamp left, Timestamp right) {
    return left.before(right) ? left : right;
  }

  public static Timestamp max(Timestamp left, Timestamp right) {
    return left.after(right) ? left : right;
  }

  @Override
  public void setTime(long time) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setYear(int year) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMonth(int month) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDate(int date) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setHours(int hours) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMinutes(int minutes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSeconds(int seconds) {
    throw new UnsupportedOperationException();
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
  public Instant toInstant() {
    return instant;
  }

  @Override
  public String toString() {
    return instant.toString();
  }

  public long toEpochMillis() {
    return instant.toEpochMilli();
  }
}
