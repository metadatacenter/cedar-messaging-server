package org.metadatacenter.messaging.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "message",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cid"}, name = "UK_message_cid")
)
public class PersistentMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @JsonProperty("@id")
  private String cid;

  @Lob
  @Column(length = 1024)
  private String subject;

  @Lob
  private String body;

  private ZonedDateTime creationDate;

  private ZonedDateTime expirationDate;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "message_sender",
      referencedColumnName = "id",
      unique = false,
      nullable = true,
      insertable = true,
      updatable = true
  )
  private PersistentMessageSender sender;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "message_recipient",
      referencedColumnName = "id",
      unique = false,
      nullable = true,
      insertable = true,
      updatable = true
  )
  private PersistentMessageRecipient recipient;

  public PersistentMessage() {
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

  public Long getId() {
    return id;
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

  public ZonedDateTime getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(ZonedDateTime creationDate) {
    this.creationDate = creationDate;
  }

  public ZonedDateTime getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(ZonedDateTime expirationDate) {
    this.expirationDate = expirationDate;
  }

  public PersistentMessageSender getSender() {
    return sender;
  }

  public void setSender(PersistentMessageSender sender) {
    this.sender = sender;
  }

  public PersistentMessageRecipient getRecipient() {
    return recipient;
  }

  public void setRecipient(PersistentMessageRecipient recipient) {
    this.recipient = recipient;
  }
}
