package io.sharetrace.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IdentifiersTests {

  @Test
  public void ofIntStringReturnsIntAsString() {
    Assertions.assertDoesNotThrow(() -> Integer.parseInt(Identifiers.ofIntString()));
  }

  @Test
  public void ofLongStringReturnsLongAsString() {
    Assertions.assertDoesNotThrow(() -> Long.parseLong(Identifiers.ofLongString()));
  }
}
