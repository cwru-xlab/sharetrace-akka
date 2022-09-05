package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.message.MessageParameters;

public interface MessageParametersBuilder extends CacheParametersBuilder {

  CacheParametersBuilder messageParameters(MessageParameters parameters);

  CacheParametersBuilder messageParameters(
      Function<MessageParametersContext, MessageParameters> factory);
}
