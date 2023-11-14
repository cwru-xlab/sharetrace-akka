package sharetrace.logging;

import com.fasterxml.jackson.core.JsonFactory;
import net.logstash.logback.decorate.JsonFactoryDecorator;

public record ShareTraceJsonFactoryDecorator() implements JsonFactoryDecorator {

  @Override
  public JsonFactory decorate(JsonFactory factory) {
    return Jackson.ionJsonFactory();
  }
}
