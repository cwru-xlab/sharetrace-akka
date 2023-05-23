package io.sharetrace.util.logging.event;

public interface IncomingMessageEvent extends MessageEvent {

  String sender();
}
