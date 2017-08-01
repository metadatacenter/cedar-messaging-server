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

}