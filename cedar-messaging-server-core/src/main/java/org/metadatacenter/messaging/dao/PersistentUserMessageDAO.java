package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentUserMessage;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class PersistentUserMessageDAO extends AbstractDAO<PersistentUserMessage> {

  public PersistentUserMessageDAO(SessionFactory factory) {
    super(factory);
  }

  public Long create(PersistentUserMessage persistentUserMessage) {
    return persist(persistentUserMessage).getId();
  }

  public PersistentUserMessage findByCid(String id) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<PersistentUserMessage> query = builder.createQuery(PersistentUserMessage.class);
    Root<PersistentUserMessage> root = query.from(PersistentUserMessage.class);
    query.select(root);
    query.where(builder.equal(root.get("cid"), id));
    Query<PersistentUserMessage> q = currentSession().createQuery(query);
    return q.uniqueResult();
  }

  public PersistentUserMessage update(PersistentUserMessage pum) {
    return persist(pum);
  }
}