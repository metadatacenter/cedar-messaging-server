package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentMessageSender;
import org.metadatacenter.messaging.model.PersistentMessageSenderProcessId;
import org.metadatacenter.messaging.model.PersistentMessageSenderType;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

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

  /**
   * The user sender row for this cid, inserting one if no request has already. See
   * {@link PersistentUserDAO#findOrCreateByCid} for why looking up and then inserting is not enough,
   * and why the violation cannot be caught once it has happened.
   * <p>
   * Only the user senders go through this. A process sender is keyed by its process rather than by a
   * cid it brings with it, and nothing in the schema makes one process hold one row.
   */
  public PersistentMessageSender findOrCreateByCid(String cid, PersistentMessageSenderType senderType) {
    PersistentMessageSender existing = findByCid(cid);
    if (existing != null) {
      return existing;
    }
    currentSession()
        .createNativeMutationQuery("INSERT INTO message_sender (cid, senderType) VALUES (:cid, :senderType)"
            + " ON DUPLICATE KEY UPDATE cid = cid")
        .setParameter("cid", cid)
        .setParameter("senderType", senderType.name())
        .executeUpdate();
    return findByCid(cid);
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