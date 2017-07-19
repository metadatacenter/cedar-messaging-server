package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PersistentMessageRecipientType {

  BROADCAST("broadcast"),
  USER("user");

  private final String value;

  PersistentMessageRecipientType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static PersistentMessageRecipientType forValue(String value) {
    for (PersistentMessageRecipientType t : values()) {
      if (t.getValue().equals(value)) {
        return t;
      }
    }
    return null;
  }
}
