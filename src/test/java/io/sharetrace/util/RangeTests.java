package io.sharetrace.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RangeTests {

  @Test
  public void bigDecimalAscending() {
    Assertions.assertEquals(
        Ranges.expectedBigDecimalAscending(), Ranges.toList(Ranges.bigDecimalAscending()));
  }

  @Test
  public void bigDecimalDescending() {
    Assertions.assertEquals(
        Ranges.expectedBigDecimalDescending(), Ranges.toList(Ranges.bigDecimalDescending()));
  }

  @Test
  public void bigDecimalStartStop() {
    Assertions.assertEquals(
        Ranges.expectedBigDecimalStartStop(), Ranges.toList(Ranges.bigDecimalStartStop()));
  }

  @Test
  public void bigDecimalStop() {
    Assertions.assertEquals(
        Ranges.expectedBigDecimalStop(), Ranges.toList(Ranges.bigDecimalStop()));
  }

  @Test
  public void doubleAscending() {
    Assertions.assertEquals(
        Ranges.expectedDoubleAscending(), Ranges.toList(Ranges.doubleAscending()));
  }

  @Test
  public void doubleDescending() {
    Assertions.assertEquals(
        Ranges.expectedDoubleDescending(), Ranges.toList(Ranges.doubleDescending()));
  }

  @Test
  public void doubleStartStop() {
    Assertions.assertEquals(
        Ranges.expectedDoubleStartStop(), Ranges.toList(Ranges.doubleStartStop()));
  }

  @Test
  public void doubleStop() {
    Assertions.assertEquals(Ranges.expectedDoubleStop(), Ranges.toList(Ranges.doubleStop()));
  }

  @Test
  public void doubleOverflow() {
    Assertions.assertThrows(
        NumberFormatException.class, () -> Ranges.toList(Ranges.doubleOverflow()));
  }

  @Test
  public void doubleUnderflow() {
    Assertions.assertThrows(
        NumberFormatException.class, () -> Ranges.toList(Ranges.doubleUnderflow()));
  }

  @Test
  public void floatAscending() {
    Assertions.assertEquals(
        Ranges.expectedFloatAscending(), Ranges.toList(Ranges.floatAscending()));
  }

  @Test
  public void floatDescending() {
    Assertions.assertEquals(
        Ranges.expectedFloatDescending(), Ranges.toList(Ranges.floatDescending()));
  }

  @Test
  public void floatStartStop() {
    Assertions.assertEquals(
        Ranges.expectedFloatStartStop(), Ranges.toList(Ranges.floatStartStop()));
  }

  @Test
  public void floatStop() {
    Assertions.assertEquals(Ranges.expectedFloatStop(), Ranges.toList(Ranges.floatStop()));
  }

  @Test
  public void floatOverflow() {
    Assertions.assertThrows(
        NumberFormatException.class, () -> Ranges.toList(Ranges.floatOverflow()));
  }

  @Test
  public void floatUnderflow() {
    Assertions.assertThrows(
        NumberFormatException.class, () -> Ranges.toList(Ranges.floatUnderflow()));
  }

  @Test
  public void longAscending() {
    Assertions.assertEquals(Ranges.expectedLongAscending(), Ranges.toList(Ranges.longAscending()));
  }

  @Test
  public void longDescending() {
    Assertions.assertEquals(
        Ranges.expectedLongDescending(), Ranges.toList(Ranges.longDescending()));
  }

  @Test
  public void longStartStop() {
    Assertions.assertEquals(Ranges.expectedLongStartStop(), Ranges.toList(Ranges.longStartStop()));
  }

  @Test
  public void longStop() {
    Assertions.assertEquals(Ranges.expectedLongStop(), Ranges.toList(Ranges.longStop()));
  }

  @Test
  public void intAscending() {
    Assertions.assertEquals(Ranges.expectedIntAscending(), Ranges.toList(Ranges.intAscending()));
  }

  @Test
  public void intDescending() {
    Assertions.assertEquals(Ranges.expectedIntDescending(), Ranges.toList(Ranges.intDescending()));
  }

  @Test
  public void intStartStop() {
    Assertions.assertEquals(Ranges.expectedIntStartStop(), Ranges.toList(Ranges.intStartStop()));
  }

  @Test
  public void intStop() {
    Assertions.assertEquals(Ranges.expectedIntStop(), Ranges.toList(Ranges.intStop()));
  }

  @Test
  public void intOverflow() {
    Assertions.assertThrows(ArithmeticException.class, () -> Ranges.toList(Ranges.intOverflow()));
  }

  @Test
  public void intUnderflow() {
    Assertions.assertThrows(ArithmeticException.class, () -> Ranges.toList(Ranges.intUnderflow()));
  }

  @Test
  public void shortAscending() {
    Assertions.assertEquals(
        Ranges.expectedShortAscending(), Ranges.toList(Ranges.shortAscending()));
  }

  @Test
  public void shortDescending() {
    Assertions.assertEquals(
        Ranges.expectedShortDescending(), Ranges.toList(Ranges.shortDescending()));
  }

  @Test
  public void shortStartStop() {
    Assertions.assertEquals(
        Ranges.expectedShortStartStop(), Ranges.toList(Ranges.shortStartStop()));
  }

  @Test
  public void shortStop() {
    Assertions.assertEquals(Ranges.expectedShortStop(), Ranges.toList(Ranges.shortStop()));
  }

  @Test
  public void shortOverflow() {
    Assertions.assertThrows(ArithmeticException.class, () -> Ranges.toList(Ranges.shortOverflow()));
  }

  @Test
  public void shortUnderflow() {
    Assertions.assertThrows(
        ArithmeticException.class, () -> Ranges.toList(Ranges.shortUnderflow()));
  }

  @Test
  public void byteAscending() {
    Assertions.assertEquals(Ranges.expectedByteAscending(), Ranges.toList(Ranges.byteAscending()));
  }

  @Test
  public void byteDescending() {
    Assertions.assertEquals(
        Ranges.expectedByteDescending(), Ranges.toList(Ranges.byteDescending()));
  }

  @Test
  public void byteStartStop() {
    Assertions.assertEquals(Ranges.expectedByteStartStop(), Ranges.toList(Ranges.byteStartStop()));
  }

  @Test
  public void byteStop() {
    Assertions.assertEquals(Ranges.expectedByteStop(), Ranges.toList(Ranges.byteStop()));
  }

  @Test
  public void byteOverflow() {
    Assertions.assertThrows(ArithmeticException.class, () -> Ranges.toList(Ranges.byteOverflow()));
  }

  @Test
  public void byteUnderflow() {
    Assertions.assertThrows(ArithmeticException.class, () -> Ranges.toList(Ranges.byteUnderflow()));
  }
}
