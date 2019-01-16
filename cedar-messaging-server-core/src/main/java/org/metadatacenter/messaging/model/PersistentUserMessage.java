package org.metadatacenter.messaging.model;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;

@Entity
@Table(name = "user_message",
    indexes = {
        @Index(columnList = "cid", name = "IDX_cid"),
        @Index(columnList = "readStatus", name = "IDX_readStatus"),
        @Index(columnList = "notificationStatus", name = "IDX_notificationStatus"),
    })
public class PersistentUserMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String cid;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "user_message_user",
      referencedColumnName = "id",
      unique = false,
      nullable = true,
      insertable = true,
      updatable = true
  )
  private PersistentUser user;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "user_message_message",
      referencedColumnName = "id",
      unique = false,
      nullable = true,
      insertable = true,
      updatable = true
  )
  private PersistentMessage message;

  @Enumerated(EnumType.STRING)
  private PersistentUserMessageReadStatus readStatus;

  @Enumerated(EnumType.STRING)
  private PersistentUserMessageNotificationStatus notificationStatus;

  public PersistentUserMessage() {
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

  public PersistentUser getUser() {
    return user;
  }

  public void setUser(PersistentUser user) {
    this.user = user;
  }

  public PersistentMessage getMessage() {
    return message;
  }

  public void setMessage(PersistentMessage message) {
    this.message = message;
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
}
