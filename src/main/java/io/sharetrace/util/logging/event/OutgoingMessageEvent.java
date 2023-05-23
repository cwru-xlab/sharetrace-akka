package io.sharetrace.util.logging.event;

public interface OutgoingMessageEvent extends MessageEvent {

  String receiver();
}
