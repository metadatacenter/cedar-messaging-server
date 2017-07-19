package org.metadatacenter.messaging.model;

import javax.persistence.*;

@Entity
@Table(name = "message_sender",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cid"}, name = "UK_message_sender_cid")
)
public class PersistentMessageSender {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String cid;

  @Enumerated(EnumType.STRING)
  private PersistentMessageSenderType senderType;

  private String screenNameKey;

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

  public PersistentMessageSenderType getSenderType() {
    return senderType;
  }

  public void setSenderType(PersistentMessageSenderType senderType) {
    this.senderType = senderType;
  }

  public String getScreenNameKey() {
    return screenNameKey;
  }

  public void setScreenNameKey(String screenNameKey) {
    this.screenNameKey = screenNameKey;
  }
}
