package sharetrace.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IdFactoryTests {

  @Test
  public void ofIntStringReturnsIntAsString() {
    Assertions.assertDoesNotThrow(() -> Integer.parseInt(IdFactory.newIntString()));
  }

  @Test
  public void ofLongStringReturnsLongAsString() {
    Assertions.assertDoesNotThrow(() -> Long.parseLong(IdFactory.newLongString()));
  }
}
