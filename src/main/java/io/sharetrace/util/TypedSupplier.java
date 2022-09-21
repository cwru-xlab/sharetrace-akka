package io.sharetrace.util;

import java.util.function.Supplier;

public final class TypedSupplier<T> implements Supplier<T> {

  private final Class<T> type;
  private final Supplier<? extends T> supplier;

  private TypedSupplier(Class<T> type, Supplier<? extends T> supplier) {
    this.type = type;
    this.supplier = supplier;
  }

  @SuppressWarnings("unchecked")
  public static <T> TypedSupplier<T> of(T result) {
    return new TypedSupplier<>((Class<T>) result.getClass(), () -> result);
  }

  public static <T> TypedSupplier<T> of(Class<T> type, Supplier<? extends T> supplier) {
    return new TypedSupplier<>(type, supplier);
  }

  @Override
  public T get() {
    return type.cast(supplier.get());
  }

  public Class<T> getType() {
    return type;
  }
}
