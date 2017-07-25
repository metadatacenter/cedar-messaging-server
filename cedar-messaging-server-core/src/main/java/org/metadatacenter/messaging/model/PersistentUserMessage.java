package org.metadatacenter.messaging.model;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;

@Entity
@Table(name = "user_message")
public class PersistentUserMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

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
  private PersistentUserMessageStatus status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public PersistentUserMessageStatus getStatus() {
    return status;
  }

  public void setStatus(PersistentUserMessageStatus status) {
    this.status = status;
  }
}
