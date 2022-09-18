package io.sharetrace.experiment.config;

public final class MissingConfigException extends IllegalStateException {

  private static final String MSG_TEMPLATE = "'%s' config property is missing";

  public MissingConfigException(String property) {
    super(String.format(MSG_TEMPLATE, property));
  }
}
