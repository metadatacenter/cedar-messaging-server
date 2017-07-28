package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentMessage;
import org.metadatacenter.messaging.model.PersistentUser;
import org.metadatacenter.messaging.model.PersistentUserMessage;
import org.metadatacenter.messaging.model.PersistentUserMessageNotificationStatus;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersistentMessageDAO extends AbstractDAO<PersistentMessage> {

  public PersistentMessageDAO(SessionFactory factory) {
    super(factory);
  }

  public Long create(PersistentMessage persistentMessage) {
    return persist(persistentMessage).getId();
  }

  public List<PersistentUserMessage> listForUser(String userId, PersistentUserMessageNotificationStatus
      notificationStatus) {
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
    query.where(andPredicates.toArray(new Predicate[andPredicates.size()]));
    query.orderBy(builder.desc(messageJoin.get("creationDate")));
    Query<PersistentUserMessage> q = currentSession().createQuery(query);
    return q.list();
  }
}