package org.sharetrace.logging.decorators;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import net.logstash.logback.decorate.JsonFactoryDecorator;

public class Iso8601DateDecorator implements JsonFactoryDecorator {

  @Override
  public JsonFactory decorate(JsonFactory factory) {
    ObjectMapper codec = (ObjectMapper) factory.getCodec();
    codec.setDateFormat(new StdDateFormat());
    return factory;
  }
}
