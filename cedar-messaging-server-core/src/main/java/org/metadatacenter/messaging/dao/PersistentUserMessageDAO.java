package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.metadatacenter.messaging.model.PersistentUserMessage;

public class PersistentUserMessageDAO extends AbstractDAO<PersistentUserMessage> {

  public PersistentUserMessageDAO(SessionFactory factory) {
    super(factory);
  }

  public Long create(PersistentUserMessage persistentUserMessage) {
    return persist(persistentUserMessage).getId();
  }
}