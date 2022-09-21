package io.sharetrace.util;

public final class TypedSuppliers {

  private static final int RESULT = 1;

  private TypedSuppliers() {}

  public static TypedSupplier<Integer> ofNull() {
    return TypedSupplier.of(Integer.class, () -> null);
  }

  public static int result() {
    return RESULT;
  }

  public static TypedSupplier<Integer> ofSupplier() {
    return TypedSupplier.of(Integer.class, () -> RESULT);
  }

  public static TypedSupplier<Integer> ofResult() {
    return TypedSupplier.of(RESULT);
  }
}
