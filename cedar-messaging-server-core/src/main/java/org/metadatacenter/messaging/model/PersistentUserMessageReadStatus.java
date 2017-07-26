package org.metadatacenter.messaging.model;

public enum PersistentUserMessageReadStatus {

  UNREAD("unread"),
  READ("read");

  private final String value;

  PersistentUserMessageReadStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
