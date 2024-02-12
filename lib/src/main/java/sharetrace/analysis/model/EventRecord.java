package sharetrace.analysis.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import sharetrace.logging.event.Event;

public record EventRecord(@JsonAlias("k") String key, @JsonAlias("e") Event event) {}
