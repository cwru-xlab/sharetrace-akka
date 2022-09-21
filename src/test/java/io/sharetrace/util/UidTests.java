package io.sharetrace.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UidTests {

  @Test
  public void ofIntStringReturnsIntAsString() {
    Assertions.assertDoesNotThrow(() -> Integer.parseInt(Uid.ofIntString()));
  }

  @Test
  public void ofLongStringReturnsLongAsString() {
    Assertions.assertDoesNotThrow(() -> Long.parseLong(Uid.ofLongString()));
  }
}