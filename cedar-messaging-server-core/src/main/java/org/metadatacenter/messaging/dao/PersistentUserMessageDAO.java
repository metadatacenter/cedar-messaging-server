package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.*;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

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
        builder.equal(countRoot.get("readStatus"), PersistentUserMessageReadStatus.UNREAD));
    countCriteria.select(builder.count(countRoot));
    Query<Long> q = currentSession().createQuery(countCriteria);
    return q.uniqueResult();
  }

  public Long getNotNotifiedCountForUser(String id) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
    Root<PersistentUserMessage> countRoot = countCriteria.from(PersistentUserMessage.class);
    Join<PersistentUserMessage, PersistentUser> userJoin = countRoot.join("user", JoinType.INNER);
    countCriteria.where(builder.and(
        builder.equal(userJoin.get("cid"), id)),
        builder.equal(countRoot.get("notificationStatus"), PersistentUserMessageNotificationStatus.NOTNOTIFIED));
    countCriteria.select(builder.count(countRoot));
    Query<Long> q = currentSession().createQuery(countCriteria);
    return q.uniqueResult();
  }

  public PersistentUserMessage update(PersistentUserMessage pum) {
    return persist(pum);
  }

  public List<PersistentUserMessage> listForUser(String userId, PersistentUserMessageNotificationStatus
      notificationStatus) {
    return listForUser(userId, notificationStatus, null);
  }

  public List<PersistentUserMessage> listForUser(String userId, PersistentUserMessageReadStatus readStatus) {
    return listForUser(userId, null, readStatus);
  }

  public List<PersistentUserMessage> listForUser(String userId, PersistentUserMessageNotificationStatus
      notificationStatus, PersistentUserMessageReadStatus readStatus) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<PersistentUserMessage> query = builder.createQuery(PersistentUserMessage.class);
    Root<PersistentUserMessage> rootUserMessage = query.from(PersistentUserMessage.class);
    Join<PersistentUserMessage, PersistentMessage> messageJoin = rootUserMessage.join("message", JoinType.INNER);
    Join<PersistentUserMessage, PersistentUser> userJoin = rootUserMessage.join("user", JoinType.INNER);
    query.select(rootUserMessage);
    List<Predicate> andPredicates = new ArrayList<>();
    andPredicates.add(builder.equal(userJoin.get("cid"), userId));
    if (notificationStatus != null) {
      andPredicates.add(builder.equal(rootUserMessage.get("notificationStatus"), notificationStatus));
    }
    if (readStatus != null) {
      andPredicates.add(builder.equal(rootUserMessage.get("readStatus"), readStatus));
    }
    query.where(andPredicates.toArray(new Predicate[andPredicates.size()]));
    query.orderBy(builder.desc(messageJoin.get("creationDate")));
    Query<PersistentUserMessage> q = currentSession().createQuery(query);
    return q.list();
  }

  public int markAllAsRead(String userId) {
    List<PersistentUserMessage> persistentUserMessages = listForUser(userId, PersistentUserMessageReadStatus
        .UNREAD);
    for (PersistentUserMessage pum : persistentUserMessages) {
      pum.setReadStatus(PersistentUserMessageReadStatus.READ);
      update(pum);
    }
    return persistentUserMessages.size();
  }
}