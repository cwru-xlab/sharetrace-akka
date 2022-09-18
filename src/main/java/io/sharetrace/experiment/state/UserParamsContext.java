package io.sharetrace.experiment.state;

import io.sharetrace.data.Dataset;

public interface UserParamsContext extends DatasetContext {

  Dataset dataset();
}
