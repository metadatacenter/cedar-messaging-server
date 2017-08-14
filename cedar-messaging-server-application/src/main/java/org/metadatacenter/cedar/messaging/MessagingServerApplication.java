package org.metadatacenter.cedar.messaging;

import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.messaging.health.MessagingServerHealthCheck;
import org.metadatacenter.cedar.messaging.resources.CommandResource;
import org.metadatacenter.cedar.messaging.resources.IndexResource;
import org.metadatacenter.cedar.messaging.resources.MessagesResource;
import org.metadatacenter.cedar.messaging.resources.SummaryResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.messaging.dao.*;
import org.metadatacenter.messaging.model.*;
import org.metadatacenter.model.ServerName;

public class MessagingServerApplication extends CedarMicroserviceApplication<MessagingServerConfiguration> {

  private HibernateBundle<MessagingServerConfiguration> hibernate;
  private PersistentUserDAO userDAO;
  private PersistentMessageDAO messageDAO;
  private PersistentUserMessageDAO userMessageDAO;
  private PersistentMessageSenderDAO messageSenderDAO;
  private PersistentMessageRecipientDAO messageRecipientDAO;

  public static void main(String[] args) throws Exception {
    new MessagingServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.MESSAGING;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<MessagingServerConfiguration> bootstrap, CedarConfig cedarConfig) {
    hibernate = new CedarMessagingHibernateBundle(
        cedarConfig,
        PersistentMessage.class, new Class[]{
        PersistentUser.class,
        PersistentUserMessage.class,
        PersistentMessageRecipient.class,
        PersistentMessageSender.class
    }
    );
    bootstrap.addBundle(hibernate);
  }

  public boolean isTestMode() {
    return false;
  }

  @Override
  public void initializeApp() {
    userDAO = new PersistentUserDAO(hibernate.getSessionFactory());
    messageDAO = new PersistentMessageDAO(hibernate.getSessionFactory());
    userMessageDAO = new PersistentUserMessageDAO(hibernate.getSessionFactory());
    messageSenderDAO = new PersistentMessageSenderDAO(hibernate.getSessionFactory());
    messageRecipientDAO = new PersistentMessageRecipientDAO(hibernate.getSessionFactory());
  }

  @Override
  public void runApp(MessagingServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final MessagesResource messages = new MessagesResource(cedarConfig, userDAO, messageDAO, userMessageDAO,
        messageSenderDAO, messageRecipientDAO);
    environment.jersey().register(messages);

    final SummaryResource summary = new SummaryResource(cedarConfig, userMessageDAO);
    environment.jersey().register(summary);

    final CommandResource command = new CommandResource(cedarConfig, userMessageDAO);
    environment.jersey().register(command);

    final MessagingServerHealthCheck healthCheck = new MessagingServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
