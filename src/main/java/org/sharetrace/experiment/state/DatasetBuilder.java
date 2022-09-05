package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.data.Dataset;

public interface DatasetBuilder extends UserParametersBuilder {

  UserParametersBuilder dataset(Dataset dataset);

  UserParametersBuilder dataset(Function<DatasetContext, Dataset> factory);
}
