package org.metadatacenter.messaging.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.metadatacenter.messaging.model.PersistentMessageRecipient;
import org.metadatacenter.messaging.model.PersistentMessageRecipientType;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

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

  /**
   * The row for this cid, inserting one if no request has already. See
   * {@link PersistentUserDAO#findOrCreateByCid} for why looking up and then inserting is not enough,
   * and why the violation cannot be caught once it has happened.
   */
  public PersistentMessageRecipient findOrCreateByCid(String cid, PersistentMessageRecipientType recipientType) {
    PersistentMessageRecipient existing = findByCid(cid);
    if (existing != null) {
      return existing;
    }
    currentSession()
        .createNativeMutationQuery("INSERT INTO message_recipient (cid, recipientType) VALUES (:cid, :recipientType)"
            + " ON DUPLICATE KEY UPDATE cid = cid")
        .setParameter("cid", cid)
        .setParameter("recipientType", recipientType.name())
        .executeUpdate();
    return findByCid(cid);
  }
}