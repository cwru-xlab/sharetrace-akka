package io.sharetrace.data;

import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.graph.FileContactNetwork;
import io.sharetrace.model.TimeRef;
import java.nio.file.Path;
import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseFileDataset extends AbstractDataset implements TimeRef {

  @Override
  public FileDataset withNewContactNetwork() {
    return FileDataset.builder()
        .addAllLoggable(loggable())
        .path(path())
        .refTime(refTime())
        .delimiter(delimiter())
        .scoreFactory(scoreFactory())
        .build();
  }

  protected abstract Path path();

  protected abstract String delimiter();

  @Override
  @Value.Default // Allows the contact network to be passed on to a copied instance.
  protected ContactNetwork contactNetwork() {
    return FileContactNetwork.builder()
        .addAllLoggable(loggable())
        .delimiter(delimiter())
        .path(path())
        .refTime(refTime())
        .build();
  }
}
