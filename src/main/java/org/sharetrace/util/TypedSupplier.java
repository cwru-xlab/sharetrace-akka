package org.sharetrace.util;

import java.util.function.Supplier;

public class TypedSupplier<T> implements Supplier<T> {

  private final Class<T> type;
  private final Supplier<T> supplier;

  private TypedSupplier(Class<T> type, Supplier<T> supplier) {
    this.type = type;
    this.supplier = supplier;
  }

  public static <T> TypedSupplier<T> of(Class<T> type, Supplier<T> supplier) {
    return new TypedSupplier<>(type, supplier);
  }

  @Override
  public T get() {
    return supplier.get();
  }

  public Class<T> getType() {
    return type;
  }
}
