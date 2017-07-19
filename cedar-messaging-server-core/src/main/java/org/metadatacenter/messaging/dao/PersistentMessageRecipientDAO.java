package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentMessageRecipient;

import javax.persistence.criteria.CriteriaQuery;

public class PersistentMessageRecipientDAO extends AbstractDAO<PersistentMessageRecipient> {

  public PersistentMessageRecipientDAO(SessionFactory factory) {
    super(factory);
  }

  public PersistentMessageRecipient findByCid(String id) {
    CriteriaQuery<PersistentMessageRecipient> query = currentSession().getCriteriaBuilder().createQuery
        (PersistentMessageRecipient.class);
    query.select(query.from(PersistentMessageRecipient.class));
    Query<PersistentMessageRecipient> q = currentSession().createQuery(query);
    return q.uniqueResult();
//

    /*Criteria criteria = this.currentSession().createCriteria(PersistentMessageRecipient.class);
    criteria.add(Restrictions.eq("cid", id));
    return (PersistentMessageRecipient) criteria.uniqueResult();*/
  }

  public Long create(PersistentMessageRecipient persistentMessageRecipient) {
    return persist(persistentMessageRecipient).getId();
  }
}