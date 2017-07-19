package org.metadatacenter.messaging.model;

public enum PersistentUserMessageStatus {

  UNREAD("unread"),
  READ("read");

  private final String value;

  PersistentUserMessageStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
