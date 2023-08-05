package sharetrace.logging;

import com.fasterxml.jackson.core.JsonFactory;
import net.logstash.logback.decorate.JsonFactoryDecorator;

public record LoggingFactoryDecorator() implements JsonFactoryDecorator {

  @Override
  public JsonFactory decorate(JsonFactory factory) {
    return Jackson.newJsonFactory();
  }
}
