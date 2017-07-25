package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentMessageSender;
import org.metadatacenter.messaging.model.PersistentMessageSenderProcessId;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class PersistentMessageSenderDAO extends AbstractDAO<PersistentMessageSender> {

  public PersistentMessageSenderDAO(SessionFactory factory) {
    super(factory);
  }

  public PersistentMessageSender findByCid(String id) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<PersistentMessageSender> query = builder.createQuery(PersistentMessageSender.class);
    Root<PersistentMessageSender> root = query.from(PersistentMessageSender.class);
    query.select(root);
    query.where(builder.equal(root.get("cid"), id));
    Query<PersistentMessageSender> q = currentSession().createQuery(query);
    return q.uniqueResult();
  }

  public Long create(PersistentMessageSender persistentMessageSender) {
    return persist(persistentMessageSender).getId();
  }

  public PersistentMessageSender findByProcessId(PersistentMessageSenderProcessId processId) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<PersistentMessageSender> query = builder.createQuery(PersistentMessageSender.class);
    Root<PersistentMessageSender> root = query.from(PersistentMessageSender.class);
    query.select(root);
    query.where(builder.equal(root.get("processId"), processId));
    Query<PersistentMessageSender> q = currentSession().createQuery(query);
    return q.uniqueResult();
  }
}