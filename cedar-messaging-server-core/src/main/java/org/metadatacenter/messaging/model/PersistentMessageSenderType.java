package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PersistentMessageSenderType {

  PROCESS("process"),
  USER("user");

  private final String value;

  PersistentMessageSenderType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static PersistentMessageSenderType forValue(String value) {
    for (PersistentMessageSenderType t : values()) {
      if (t.getValue().equals(value)) {
        return t;
      }
    }
    return null;
  }

}
