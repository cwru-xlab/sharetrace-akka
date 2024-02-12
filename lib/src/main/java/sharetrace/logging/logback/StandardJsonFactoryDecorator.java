package sharetrace.logging.logback;

import com.fasterxml.jackson.core.JsonFactory;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import sharetrace.logging.jackson.Jackson;

public record StandardJsonFactoryDecorator() implements JsonFactoryDecorator {

  @Override
  public JsonFactory decorate(JsonFactory factory) {
    return Jackson.jsonFactory();
  }
}
