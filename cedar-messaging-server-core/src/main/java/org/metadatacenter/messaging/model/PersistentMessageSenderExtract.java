package org.metadatacenter.messaging.model;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class PersistentMessageSenderExtract {

  private String id;

  @Enumerated(EnumType.STRING)
  private PersistentMessageSenderType senderType;

  @Enumerated(EnumType.STRING)
  private PersistentMessageSenderProcessId processId;

  private String screenName;

  public PersistentMessageSenderExtract(PersistentMessageSender sender, String screenName) {
    setId(sender.getCid());
    setProcessId(sender.getProcessId());
    setSenderType(sender.getSenderType());
    setScreenName(screenName);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PersistentMessageSenderType getSenderType() {
    return senderType;
  }

  public void setSenderType(PersistentMessageSenderType senderType) {
    this.senderType = senderType;
  }

  public PersistentMessageSenderProcessId getProcessId() {
    return processId;
  }

  public void setProcessId(PersistentMessageSenderProcessId processId) {
    this.processId = processId;
  }

  public String getScreenName() {
    return screenName;
  }

  public void setScreenName(String screenName) {
    this.screenName = screenName;
  }
}
