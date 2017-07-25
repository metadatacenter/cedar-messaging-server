package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersistentMessageRequest {

  @JsonProperty("@id")
  private String id;

  private String subject;

  private String body;

  @JsonProperty("to")
  private PersistentMessageRecipient recipient;

  @JsonProperty("from")
  private PersistentMessageSender sender;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public PersistentMessageRecipient getRecipient() {
    return recipient;
  }

  public void setRecipient(PersistentMessageRecipient recipient) {
    this.recipient = recipient;
  }

  public PersistentMessageSender getSender() {
    return sender;
  }

  public void setSender(PersistentMessageSender sender) {
    this.sender = sender;
  }
}
