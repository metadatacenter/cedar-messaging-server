package org.metadatacenter.messaging.model;

import javax.persistence.*;

@Entity
@Table(name = "message")
public class PersistentMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
