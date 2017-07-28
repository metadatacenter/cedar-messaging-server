package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PersistentUserMessageReadStatus {

  UNREAD("unread"),
  READ("read");

  private final String value;

  PersistentUserMessageReadStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static PersistentUserMessageReadStatus forValue(String value) {
    for (PersistentUserMessageReadStatus t : values()) {
      if (t.getValue().equals(value)) {
        return t;
      }
    }
    return null;
  }

}
