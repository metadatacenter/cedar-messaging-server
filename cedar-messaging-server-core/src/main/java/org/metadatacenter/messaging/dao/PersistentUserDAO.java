package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.metadatacenter.messaging.model.PersistentUser;

public class PersistentUserDAO extends AbstractDAO<PersistentUser> {

  public PersistentUserDAO(SessionFactory factory) {
    super(factory);
  }

  public PersistentUser findById(Long id) {
    return get(id);
  }

  public long create(PersistentUser user) {
    return persist(user).getId();
  }
}