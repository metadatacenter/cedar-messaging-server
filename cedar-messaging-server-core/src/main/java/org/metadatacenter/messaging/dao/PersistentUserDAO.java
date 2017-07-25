package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentMessageRecipient;
import org.metadatacenter.messaging.model.PersistentUser;
import org.metadatacenter.messaging.model.PersistentUserMessage;
import org.metadatacenter.messaging.model.PersistentUserMessageStatus;

import javax.persistence.criteria.*;

public class PersistentUserDAO extends AbstractDAO<PersistentUser> {

  public PersistentUserDAO(SessionFactory factory) {
    super(factory);
  }

  public long getTotalCountForUser(String id) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
    Root<PersistentUserMessage> countRoot = countCriteria.from(PersistentUserMessage.class);
    Join<PersistentUserMessage, PersistentUser> userJoin = countRoot.join("user", JoinType.INNER);
    countCriteria.where(builder.equal(userJoin.get("cid"), id));
    countCriteria.select(builder.count(countRoot));
    Query<Long> q = currentSession().createQuery(countCriteria);
    return q.uniqueResult();
  }

  public long getUnreadCountForUser(String id) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
    Root<PersistentUserMessage> countRoot = countCriteria.from(PersistentUserMessage.class);
    Join<PersistentUserMessage, PersistentUser> userJoin = countRoot.join("user", JoinType.INNER);
    countCriteria.where(builder.and(
        builder.equal(userJoin.get("cid"), id)),
        builder.equal(countRoot.get("status"), PersistentUserMessageStatus.UNREAD));
    countCriteria.select(builder.count(countRoot));
    Query<Long> q = currentSession().createQuery(countCriteria);
    return q.uniqueResult();
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