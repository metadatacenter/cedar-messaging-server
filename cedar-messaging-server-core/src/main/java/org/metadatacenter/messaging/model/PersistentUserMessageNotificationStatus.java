package org.metadatacenter.messaging.model;

public enum PersistentUserMessageNotificationStatus {

  NOTNOTIFIED("notnotified"),
  NOTIFIED("notified");

  private final String value;

  PersistentUserMessageNotificationStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
