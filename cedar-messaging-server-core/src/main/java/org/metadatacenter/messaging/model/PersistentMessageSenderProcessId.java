package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PersistentMessageSenderProcessId {

  NONE(null),
  SUBMISSION_IMMPORT("submission.IMMPORT"),
  SUBMISSION_NCBI("submission.NCBI");

  private final String value;

  PersistentMessageSenderProcessId(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static PersistentMessageSenderProcessId forValue(String value) {
    if (value == null) {
      return NONE;
    }
    for (PersistentMessageSenderProcessId t : values()) {
      if (value.equals(t.getValue())) {
        return t;
      }
    }
    return null;
  }

}
