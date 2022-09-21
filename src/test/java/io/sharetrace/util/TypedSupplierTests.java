package io.sharetrace.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypedSupplierTests {

  @Test
  public void ofResultGetReturnsResult() {
    Assertions.assertEquals(TypedSuppliers.ofResult().get(), TypedSuppliers.result());
  }

  @Test
  public void ofSupplierGetReturnsResult() {
    Assertions.assertEquals(TypedSuppliers.ofSupplier().get(), TypedSuppliers.result());
  }

  @Test
  public void nullResultDoesNotThrowException() {
    Assertions.assertDoesNotThrow(TypedSuppliers.ofNull()::get);
  }
}
