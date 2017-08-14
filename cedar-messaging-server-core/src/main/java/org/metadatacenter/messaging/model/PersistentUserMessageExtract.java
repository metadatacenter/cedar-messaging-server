package org.metadatacenter.messaging.model;

import org.metadatacenter.constant.CedarConstants;

public class PersistentUserMessageExtract {

  private String id;
  private PersistentUserMessageReadStatus readStatus;
  private PersistentUserMessageNotificationStatus notificationStatus;
  private String subject;
  private String body;
  private String creationDate;
  private PersistentMessageSenderExtract from;

  public PersistentUserMessageExtract(PersistentUserMessage pum, String screenName) {
    setId(pum.getCid());
    setReadStatus(pum.getReadStatus());
    setNotificationStatus(pum.getNotificationStatus());

    setSubject(pum.getMessage().getSubject());
    setBody(pum.getMessage().getBody());
    String creationDateString = CedarConstants.xsdDateTimeFormatter.format(pum.getMessage().getCreationDate());
    setCreationDate(creationDateString);

    PersistentMessageSenderExtract from = new PersistentMessageSenderExtract(pum.getMessage().getSender(), screenName);
    setFrom(from);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PersistentUserMessageReadStatus getReadStatus() {
    return readStatus;
  }

  public void setReadStatus(PersistentUserMessageReadStatus readStatus) {
    this.readStatus = readStatus;
  }

  public PersistentUserMessageNotificationStatus getNotificationStatus() {
    return notificationStatus;
  }

  public void setNotificationStatus(PersistentUserMessageNotificationStatus notificationStatus) {
    this.notificationStatus = notificationStatus;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(String creationDate) {
    this.creationDate = creationDate;
  }

  public PersistentMessageSenderExtract getFrom() {
    return from;
  }

  public void setFrom(PersistentMessageSenderExtract from) {
    this.from = from;
  }
}
