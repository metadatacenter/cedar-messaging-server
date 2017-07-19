package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.metadatacenter.messaging.model.PersistentUser;
import org.metadatacenter.messaging.model.PersistentUserMessage;
import org.metadatacenter.messaging.model.PersistentUserMessageStatus;

public class PersistentUserDAO extends AbstractDAO<PersistentUser> {

  public PersistentUserDAO(SessionFactory factory) {
    super(factory);
  }

  public long getTotalCountForUser(String id) {
    Criteria criteria = this.currentSession().createCriteria(PersistentUserMessage.class, "usermessage");
    criteria.createAlias("usermessage.recipient", "recipient", Criteria.INNER_JOIN);
    criteria.add(Restrictions.eq("recipient.cid", id));
    criteria.setProjection(Projections.rowCount());
    return (Long) criteria.uniqueResult();
  }

  public long getUnreadCountForUser(String id) {
    Criteria criteria = this.currentSession().createCriteria(PersistentUserMessage.class, "usermessage");
    criteria.createAlias("usermessage.recipient", "recipient", Criteria.INNER_JOIN);
    criteria.add(Restrictions.eq("recipient.cid", id));
    criteria.add(Restrictions.eq("status", PersistentUserMessageStatus.UNREAD));
    criteria.setProjection(Projections.rowCount());
    return (Long) criteria.uniqueResult();
  }

  public Long create(PersistentUser persistentRecipientUser) {
    return persist(persistentRecipientUser).getId();
  }

  public PersistentUser findByCid(String id) {
    Criteria criteria = this.currentSession().createCriteria(PersistentUser.class);
    criteria.add(Restrictions.eq("cid", id));
    return (PersistentUser) criteria.uniqueResult();
  }
}