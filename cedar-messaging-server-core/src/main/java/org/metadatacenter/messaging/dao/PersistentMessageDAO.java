package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.metadatacenter.messaging.model.PersistentMessage;

public class PersistentMessageDAO extends AbstractDAO<PersistentMessage> {

  public PersistentMessageDAO(SessionFactory factory) {
    super(factory);
  }

  public PersistentMessage findById(Long id) {
    return get(id);
  }

  public long create(PersistentMessage message) {
    return persist(message).getId();
  }
}