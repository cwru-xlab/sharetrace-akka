package org.sharetrace.util;

import java.util.Objects;
import java.util.function.Supplier;

public class TypedSupplier<T> implements Supplier<T> {

  private final Class<? extends T> type;
  private final Supplier<? extends T> supplier;

  private <R extends T> TypedSupplier(Class<R> type, Supplier<R> supplier) {
    this.type = Objects.requireNonNull(type);
    this.supplier = Objects.requireNonNull(supplier);
  }

  public static <T, R extends T> TypedSupplier<T> of(Class<R> type, Supplier<R> supplier) {
    return new TypedSupplier<>(type, supplier);
  }

  @SuppressWarnings("unchecked")
  public static <T> TypedSupplier<T> of(T result) {
    return new TypedSupplier<>((Class<T>) result.getClass(), () -> result);
  }

  @Override
  public T get() {
    Object result = Objects.requireNonNull(supplier.get());
    return type.cast(result);
  }

  public Class<? extends T> getType() {
    return type;
  }
}
