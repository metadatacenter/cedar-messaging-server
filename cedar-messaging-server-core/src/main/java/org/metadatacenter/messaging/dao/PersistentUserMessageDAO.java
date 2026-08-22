package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.*;

import jakarta.persistence.criteria.*;
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

  /**
   * Marks every unread message of a user read, and answers how many rows that changed.
   * <p>
   * The count comes from the statement rather than from the size of a list read beforehand, which is
   * what the caller reports as {@code updated}. Selecting the unread messages and then updating them
   * one at a time counted the rows it meant to change, not the rows it did: a message read from
   * another session in between was still counted, and each row was a statement of its own.
   * <p>
   * A bulk update does not go through the persistence context, so any {@code PersistentUserMessage}
   * already loaded in this session keeps its old read status. Nothing reads one either side of this
   * call — the endpoint returns the count alone — and a caller that needs the rows afterwards should
   * re-read them.
   */
  public int markAllAsRead(String userId) {
    return currentSession()
        .createMutationQuery("""
            UPDATE PersistentUserMessage pum
            SET pum.readStatus = :readStatus
            WHERE pum.readStatus = :unreadStatus
              AND pum.user IN (SELECT u FROM PersistentUser u WHERE u.cid = :userId)
            """)
        .setParameter("readStatus", PersistentUserMessageReadStatus.READ)
        .setParameter("unreadStatus", PersistentUserMessageReadStatus.UNREAD)
        .setParameter("userId", userId)
        .executeUpdate();
  }
}