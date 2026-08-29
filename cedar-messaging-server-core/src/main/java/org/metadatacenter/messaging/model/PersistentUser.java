package org.metadatacenter.messaging.model;

import jakarta.persistence.*;

@Entity
@Table(
    // USER is also a MariaDB system relation. Hibernate 6.6's schema metadata lookup otherwise
    // mistakes that relation for this application table, skips creating cedar_messaging.user, and
    // later fails the user_message foreign key. Quoting keeps the existing physical table name.
    name = "`user`",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cid"}, name = "UK_user_cid")
)
public class PersistentUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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
