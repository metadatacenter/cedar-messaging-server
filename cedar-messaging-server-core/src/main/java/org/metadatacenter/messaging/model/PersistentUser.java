package org.metadatacenter.messaging.model;

import javax.persistence.*;

@Entity
@Table(
    name = "user",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cid"}, name = "UK_user_cid")
)
public class PersistentUser {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String cid;

  public PersistentUser() {
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
}
