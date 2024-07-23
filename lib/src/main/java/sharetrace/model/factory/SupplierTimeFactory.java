package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.function.LongSupplier;

public record SupplierTimeFactory(@JsonIgnore LongSupplier supplier, String type)
    implements TimeFactory {

  @Override
  public long getTime() {
    return supplier.getAsLong();
  }
}
