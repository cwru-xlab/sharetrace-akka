package io.sharetrace.util;

import java.util.function.Supplier;

public final class TypedSupplier<T> implements Supplier<T> {

  private final Class<? extends T> type;
  private final Supplier<? extends T> supplier;

  private <R extends T> TypedSupplier(Class<R> type, Supplier<R> supplier) {
    this.type = type;
    this.supplier = supplier;
  }

  @SuppressWarnings("unchecked")
  public static <T> TypedSupplier<T> of(T result) {
    return new TypedSupplier<>((Class<T>) result.getClass(), () -> result);
  }

  public static <T, R extends T> TypedSupplier<T> of(Class<R> type, Supplier<R> supplier) {
    return new TypedSupplier<>(type, supplier);
  }

  @Override
  public T get() {
    return type.cast(supplier.get());
  }

  public Class<? extends T> getType() {
    return type;
  }
}
