package io.sharetrace.experiment.state;

import io.sharetrace.model.UserParams;

public interface DatasetContext extends UserParamsContext {

  UserParams userParams();
}
