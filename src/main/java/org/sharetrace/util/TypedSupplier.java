package org.sharetrace.util;

import java.util.Objects;
import java.util.function.Supplier;

public class TypedSupplier<T> implements Supplier<T> {

  private final Class<T> type;
  private final Supplier<? super T> supplier;

  private TypedSupplier(Class<T> type, Supplier<? super T> supplier) {
    this.type = Objects.requireNonNull(type);
    this.supplier = Objects.requireNonNull(supplier);
  }

  @SuppressWarnings("unchecked")
  public static <T> TypedSupplier<T> of(T result) {
    return new TypedSupplier<>((Class<T>) result.getClass(), () -> result);
  }

  public static <T> TypedSupplier<T> of(Class<T> type, Supplier<? super T> supplier) {
    return new TypedSupplier<>(type, supplier);
  }

  /**
   * Returns a result of the {@link Supplier} if it is an instance of the expected type.
   *
   * @throws ClassCastException when the result not assignable to the type T.
   * @throws NullPointerException when the result is null
   */
  @Override
  public T get() {
    Object result = Objects.requireNonNull(supplier.get());
    return type.cast(result);
  }

  /** Returns the expected class type of the return value of {@link Supplier#get()}. */
  public Class<T> getType() {
    return type;
  }
}
