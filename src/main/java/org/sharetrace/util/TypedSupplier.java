package org.sharetrace.util;

import java.util.Objects;
import java.util.function.Supplier;

public class TypedSupplier<T> implements Supplier<T> {

  private final Class<? extends T> type;
  private final Supplier<T> supplier;

  private TypedSupplier(Class<? extends T> type, Supplier<T> supplier) {
    this.type = Objects.requireNonNull(type);
    this.supplier = Objects.requireNonNull(supplier);
  }

  public static <T> TypedSupplier<T> of(Class<? extends T> type, Supplier<T> supplier) {
    return new TypedSupplier<>(type, supplier);
  }

  public static <T> TypedSupplier<T> of(Class<? extends T> type, T result) {
    return new TypedSupplier<>(type, () -> result);
  }

  /**
   * Returns a result of the {@link Supplier} and checks that its type matches the expected type.
   *
   * @throws IllegalArgumentException when the result type differs from the expected type
   * @throws NullPointerException when the result is null
   */
  @Override
  public T get() {
    T result = Objects.requireNonNull(supplier.get());
    Class<?> resultType = result.getClass();
    if (!resultType.equals(type)) {
      throw new IllegalStateException(
          "Result type " + resultType + " differs from the expected type " + type);
    }
    return result;
  }

  /** Returns the expected class type of the return value of {@link Supplier#get()}. */
  public Class<? extends T> getType() {
    return type;
  }
}
