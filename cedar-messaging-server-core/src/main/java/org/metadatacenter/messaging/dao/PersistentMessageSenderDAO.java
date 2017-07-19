package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.metadatacenter.messaging.model.PersistentMessageSender;
import org.metadatacenter.messaging.model.PersistentUserMessage;

public class PersistentMessageSenderDAO extends AbstractDAO<PersistentMessageSender> {

  public PersistentMessageSenderDAO(SessionFactory factory) {
    super(factory);
  }

  public PersistentMessageSender findByCid(String id) {
    Criteria criteria = this.currentSession().createCriteria(PersistentMessageSender.class);
    criteria.add(Restrictions.eq("cid", id));
    return (PersistentMessageSender) criteria.uniqueResult();
  }

  public Long create(PersistentMessageSender persistentMessageSender) {
    return persist(persistentMessageSender).getId();
  }
}