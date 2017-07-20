package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentMessageRecipient;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class PersistentMessageRecipientDAO extends AbstractDAO<PersistentMessageRecipient> {

  public PersistentMessageRecipientDAO(SessionFactory factory) {
    super(factory);
  }

  public PersistentMessageRecipient findByCid(String id) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<PersistentMessageRecipient> query = builder.createQuery(PersistentMessageRecipient.class);
    Root<PersistentMessageRecipient> root = query.from(PersistentMessageRecipient.class);
    query.select(root);
    query.where(builder.equal(root.get("cid"), id));
    Query<PersistentMessageRecipient> q = currentSession().createQuery(query);
    return q.uniqueResult();
  }

  public Long create(PersistentMessageRecipient persistentMessageRecipient) {
    return persist(persistentMessageRecipient).getId();
  }
}