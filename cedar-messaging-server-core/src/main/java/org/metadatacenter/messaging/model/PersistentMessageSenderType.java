package org.metadatacenter.messaging.model;

public enum PersistentMessageSenderType {

  PROCESS("process"),
  USER("user");

  private final String value;

  PersistentMessageSenderType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
