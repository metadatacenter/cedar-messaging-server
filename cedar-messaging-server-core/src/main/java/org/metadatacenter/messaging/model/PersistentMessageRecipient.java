package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "message_recipient",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cid"}, name = "UK_message_recipient_cid")
)
public class PersistentMessageRecipient {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @JsonProperty("@id")
  private String cid;

  @Enumerated(EnumType.STRING)
  private PersistentMessageRecipientType recipientType;

  public PersistentMessageRecipient() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCid() {
    return cid;
  }

  public void setCid(String cid) {
    this.cid = cid;
  }

  public PersistentMessageRecipientType getRecipientType() {
    return recipientType;
  }

  public void setRecipientType(PersistentMessageRecipientType recipientType) {
    this.recipientType = recipientType;
  }

}
