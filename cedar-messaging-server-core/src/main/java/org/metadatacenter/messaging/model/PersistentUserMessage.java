package org.metadatacenter.messaging.model;

import javax.persistence.*;

@Entity
@Table(name = "user_message")
public class PersistentUserMessage {

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
