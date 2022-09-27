package io.sharetrace.experiment.state;

import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
interface BaseExperimentContext extends AbstractExperimentContext {}
