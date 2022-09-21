package io.sharetrace.util;

import io.sharetrace.util.range.BigDecimalRange;
import io.sharetrace.util.range.ByteRange;
import io.sharetrace.util.range.DoubleRange;
import io.sharetrace.util.range.FloatRange;
import io.sharetrace.util.range.IntRange;
import io.sharetrace.util.range.LongRange;
import io.sharetrace.util.range.Range;
import io.sharetrace.util.range.ShortRange;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Ranges {

  private static final BigDecimal BIG_DECIMAL_START = BigDecimal.valueOf(1.0);
  private static final BigDecimal BIG_DECIMAL_STOP = BigDecimal.valueOf(3.0);
  private static final BigDecimal BIG_DECIMAL_POSITIVE_STEP = BigDecimal.valueOf(0.5);
  private static final BigDecimal BIG_DECIMAL_NEGATIVE_STEP = BigDecimal.valueOf(-0.5);
  private static final double DECIMAL_START = BIG_DECIMAL_START.doubleValue();
  private static final double DECIMAL_STOP = BIG_DECIMAL_STOP.doubleValue();
  private static final double DECIMAL_POSITIVE_STEP = BIG_DECIMAL_POSITIVE_STEP.doubleValue();
  private static final double DECIMAL_NEGATIVE_STEP = BIG_DECIMAL_NEGATIVE_STEP.doubleValue();
  private static final long INTEGRAL_START = 1L;
  private static final long INTEGRAL_STOP = 6L;
  private static final long INTEGRAL_POSITIVE_STEP = 2L;
  private static final long INTEGRAL_NEGATIVE_STEP = -2L;

  private Ranges() {}

  public static List<Long> expectedLongAscending() {
    return List.of(1L, 3L, 5L);
  }

  public static List<Long> expectedLongDescending() {
    return List.of(6L, 4L, 2L);
  }

  public static List<Long> expectedLongStartStop() {
    return List.of(1L, 2L, 3L, 4L, 5L);
  }

  public static List<Long> expectedLongStop() {
    return List.of(0L, 1L, 2L, 3L, 4L, 5L);
  }

  public static List<Long> expectedLongSingle() {
    return List.of(1L);
  }

  public static List<Integer> expectedIntAscending() {
    return map(expectedLongAscending(), Number::intValue);
  }

  public static List<Integer> expectedIntDescending() {
    return map(expectedLongDescending(), Number::intValue);
  }

  public static List<Integer> expectedIntStartStop() {
    return map(expectedLongStartStop(), Number::intValue);
  }

  public static List<Integer> expectedIntStop() {
    return map(expectedLongStop(), Number::intValue);
  }

  public static List<Integer> expectedIntSingle() {
    return map(expectedLongSingle(), Number::intValue);
  }

  public static List<Short> expectedShortAscending() {
    return map(expectedLongAscending(), Number::shortValue);
  }

  public static List<Short> expectedShortDescending() {
    return map(expectedLongDescending(), Number::shortValue);
  }

  public static List<Short> expectedShortStartStop() {
    return map(expectedLongStartStop(), Number::shortValue);
  }

  public static List<Short> expectedShortStop() {
    return map(expectedLongStop(), Number::shortValue);
  }

  public static List<Short> expectedShortSingle() {
    return map(expectedLongSingle(), Number::shortValue);
  }

  public static List<Byte> expectedByteAscending() {
    return map(expectedLongAscending(), Number::byteValue);
  }

  public static List<Byte> expectedByteDescending() {
    return map(expectedLongDescending(), Number::byteValue);
  }

  public static List<Byte> expectedByteStartStop() {
    return map(expectedLongStartStop(), Number::byteValue);
  }

  public static List<Byte> expectedByteStop() {
    return map(expectedLongStop(), Number::byteValue);
  }

  public static List<Byte> expectedByteSingle() {
    return map(expectedLongSingle(), Number::byteValue);
  }

  public static Range<Long> longAscending() {
    return LongRange.of(INTEGRAL_START, INTEGRAL_STOP, INTEGRAL_POSITIVE_STEP);
  }

  public static Range<Long> longDescending() {
    return LongRange.of(INTEGRAL_STOP, INTEGRAL_START, INTEGRAL_NEGATIVE_STEP);
  }

  public static Range<Long> longStartStop() {
    return LongRange.of(INTEGRAL_START, INTEGRAL_STOP);
  }

  public static Range<Long> longStop() {
    return LongRange.of(INTEGRAL_STOP);
  }

  public static Range<Long> longSingle() {
    return LongRange.single(INTEGRAL_START);
  }

  public static Range<Integer> intAscending() {
    return IntRange.of(INTEGRAL_START, INTEGRAL_STOP, INTEGRAL_POSITIVE_STEP);
  }

  public static Range<Integer> intDescending() {
    return IntRange.of(INTEGRAL_STOP, INTEGRAL_START, INTEGRAL_NEGATIVE_STEP);
  }

  public static Range<Integer> intStartStop() {
    return IntRange.of(INTEGRAL_START, INTEGRAL_STOP);
  }

  public static Range<Integer> intStop() {
    return IntRange.of(INTEGRAL_STOP);
  }

  public static Range<Integer> intSingle() {
    return IntRange.single(INTEGRAL_START);
  }

  public static Range<Integer> intOverflow() {
    return IntRange.of(Integer.MAX_VALUE, Integer.MAX_VALUE + 2L);
  }

  public static Range<Integer> intUnderflow() {
    return IntRange.of(Integer.MIN_VALUE - 1L, Integer.MIN_VALUE);
  }

  public static Range<Short> shortAscending() {
    return ShortRange.of(INTEGRAL_START, INTEGRAL_STOP, INTEGRAL_POSITIVE_STEP);
  }

  public static Range<Short> shortDescending() {
    return ShortRange.of(INTEGRAL_STOP, INTEGRAL_START, INTEGRAL_NEGATIVE_STEP);
  }

  public static Range<Short> shortStartStop() {
    return ShortRange.of(INTEGRAL_START, INTEGRAL_STOP);
  }

  public static Range<Short> shortStop() {
    return ShortRange.of(INTEGRAL_STOP);
  }

  public static Range<Short> shortSingle() {
    return ShortRange.single(INTEGRAL_START);
  }

  public static Range<Short> shortOverflow() {
    return ShortRange.of(Short.MAX_VALUE, Short.MAX_VALUE + 2L);
  }

  public static Range<Short> shortUnderflow() {
    return ShortRange.of(Short.MIN_VALUE - 1L, Short.MIN_VALUE);
  }

  public static Range<Byte> byteAscending() {
    return ByteRange.of(INTEGRAL_START, INTEGRAL_STOP, INTEGRAL_POSITIVE_STEP);
  }

  public static Range<Byte> byteDescending() {
    return ByteRange.of(INTEGRAL_STOP, INTEGRAL_START, INTEGRAL_NEGATIVE_STEP);
  }

  public static Range<Byte> byteStartStop() {
    return ByteRange.of(INTEGRAL_START, INTEGRAL_STOP);
  }

  public static Range<Byte> byteStop() {
    return ByteRange.of(INTEGRAL_STOP);
  }

  public static Range<Byte> byteSingle() {
    return ByteRange.single(INTEGRAL_START);
  }

  public static Range<Byte> byteOverflow() {
    return ByteRange.of(Byte.MAX_VALUE, Byte.MAX_VALUE + 2L);
  }

  public static Range<Byte> byteUnderflow() {
    return ByteRange.of(Byte.MIN_VALUE - 1L, Byte.MIN_VALUE);
  }

  public static List<BigDecimal> expectedBigDecimalAscending() {
    return List.of(
        BigDecimal.valueOf(1.0),
        BigDecimal.valueOf(1.5),
        BigDecimal.valueOf(2.0),
        BigDecimal.valueOf(2.5));
  }

  public static List<BigDecimal> expectedBigDecimalDescending() {
    return List.of(
        BigDecimal.valueOf(3.0),
        BigDecimal.valueOf(2.5),
        BigDecimal.valueOf(2.0),
        BigDecimal.valueOf(1.5));
  }

  public static List<BigDecimal> expectedBigDecimalStartStop() {
    return List.of(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0));
  }

  public static List<BigDecimal> expectedBigDecimalStop() {
    return List.of(BigDecimal.valueOf(0L), BigDecimal.valueOf(1L), BigDecimal.valueOf(2L));
  }

  public static List<BigDecimal> expectedBigDecimalSingle() {
    return List.of(BigDecimal.valueOf(1.0));
  }

  public static List<Double> expectedDoubleAscending() {
    return map(expectedBigDecimalAscending(), BigDecimal::doubleValue);
  }

  public static List<Double> expectedDoubleDescending() {
    return map(expectedBigDecimalDescending(), BigDecimal::doubleValue);
  }

  public static List<Double> expectedDoubleStartStop() {
    return map(expectedBigDecimalStartStop(), BigDecimal::doubleValue);
  }

  public static List<Double> expectedDoubleStop() {
    return map(expectedBigDecimalStop(), BigDecimal::doubleValue);
  }

  public static List<Double> expectedDoubleSingle() {
    return map(expectedBigDecimalSingle(), BigDecimal::doubleValue);
  }

  public static List<Float> expectedFloatAscending() {
    return map(expectedBigDecimalAscending(), BigDecimal::floatValue);
  }

  public static List<Float> expectedFloatDescending() {
    return map(expectedBigDecimalDescending(), BigDecimal::floatValue);
  }

  public static List<Float> expectedFloatStartStop() {
    return map(expectedBigDecimalStartStop(), BigDecimal::floatValue);
  }

  public static List<Float> expectedFloatStop() {
    return map(expectedBigDecimalStop(), BigDecimal::floatValue);
  }

  public static List<Float> expectedFloatSingle() {
    return map(expectedBigDecimalSingle(), BigDecimal::floatValue);
  }

  public static Range<BigDecimal> bigDecimalAscending() {
    return BigDecimalRange.of(BIG_DECIMAL_START, BIG_DECIMAL_STOP, BIG_DECIMAL_POSITIVE_STEP);
  }

  public static Range<BigDecimal> bigDecimalDescending() {
    return BigDecimalRange.of(BIG_DECIMAL_STOP, BIG_DECIMAL_START, BIG_DECIMAL_NEGATIVE_STEP);
  }

  public static Range<BigDecimal> bigDecimalStartStop() {
    return BigDecimalRange.of(BIG_DECIMAL_START, BIG_DECIMAL_STOP);
  }

  public static Range<BigDecimal> bigDecimalStop() {
    return BigDecimalRange.of(BIG_DECIMAL_STOP);
  }

  public static Range<BigDecimal> bigDecimalSingle() {
    return BigDecimalRange.single(BIG_DECIMAL_START);
  }

  public static Range<Double> doubleAscending() {
    return DoubleRange.of(DECIMAL_START, DECIMAL_STOP, DECIMAL_POSITIVE_STEP);
  }

  public static Range<Double> doubleDescending() {
    return DoubleRange.of(DECIMAL_STOP, DECIMAL_START, DECIMAL_NEGATIVE_STEP);
  }

  public static Range<Double> doubleStartStop() {
    return DoubleRange.of(DECIMAL_START, DECIMAL_STOP);
  }

  public static Range<Double> doubleStop() {
    return DoubleRange.of(DECIMAL_STOP);
  }

  public static Range<Double> doubleSingle() {
    return DoubleRange.single(DECIMAL_START);
  }

  public static Range<Double> doubleOverflow() {
    return DoubleRange.of(Double.MAX_VALUE, Double.POSITIVE_INFINITY);
  }

  public static Range<Double> doubleUnderflow() {
    return DoubleRange.of(Double.MIN_VALUE, Double.NEGATIVE_INFINITY, -1d);
  }

  public static Range<Float> floatAscending() {
    return FloatRange.of(DECIMAL_START, DECIMAL_STOP, DECIMAL_POSITIVE_STEP);
  }

  public static Range<Float> floatDescending() {
    return FloatRange.of(DECIMAL_STOP, DECIMAL_START, DECIMAL_NEGATIVE_STEP);
  }

  public static Range<Float> floatStartStop() {
    return FloatRange.of(DECIMAL_START, DECIMAL_STOP);
  }

  public static Range<Float> floatStop() {
    return FloatRange.of(DECIMAL_STOP);
  }

  public static Range<Float> floatSingle() {
    return FloatRange.single(DECIMAL_START);
  }

  public static Range<Float> floatOverflow() {
    return FloatRange.of(Float.MAX_VALUE, Double.POSITIVE_INFINITY);
  }

  public static Range<Float> floatUnderflow() {
    return FloatRange.of(Float.MIN_VALUE, Double.NEGATIVE_INFINITY, -1d);
  }

  public static <N extends Number> List<N> toList(Range<N> range) {
    return range.stream().collect(Collectors.toList());
  }

  private static <T, R> List<R> map(List<T> numbers, Function<T, R> mapper) {
    return numbers.stream().map(mapper).collect(Collectors.toList());
  }
}
