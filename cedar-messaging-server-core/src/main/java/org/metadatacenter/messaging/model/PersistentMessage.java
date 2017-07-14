package org.metadatacenter.messaging.model;

import javax.persistence.*;

@Entity
@Table(name = "message",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cid"}, name = "UK_message_cid")
)
public class PersistentMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String cid;

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
}
