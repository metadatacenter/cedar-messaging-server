package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PersistentUserMessageNotificationStatus {

  NOTNOTIFIED("notnotified"),
  NOTIFIED("notified");

  private final String value;

  PersistentUserMessageNotificationStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static PersistentUserMessageNotificationStatus forValue(String value) {
    for (PersistentUserMessageNotificationStatus t : values()) {
      if (t.getValue().equals(value)) {
        return t;
      }
    }
    return null;
  }

}
