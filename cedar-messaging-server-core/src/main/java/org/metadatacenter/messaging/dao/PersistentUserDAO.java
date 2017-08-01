package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentUser;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class PersistentUserDAO extends AbstractDAO<PersistentUser> {

  public PersistentUserDAO(SessionFactory factory) {
    super(factory);
  }

  public Long create(PersistentUser persistentRecipientUser) {
    return persist(persistentRecipientUser).getId();
  }

  public PersistentUser findByCid(String id) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<PersistentUser> query = builder.createQuery(PersistentUser.class);
    Root<PersistentUser> root = query.from(PersistentUser.class);
    query.select(root);
    query.where(builder.equal(root.get("cid"), id));
    Query<PersistentUser> q = currentSession().createQuery(query);
    return q.uniqueResult();
  }
}