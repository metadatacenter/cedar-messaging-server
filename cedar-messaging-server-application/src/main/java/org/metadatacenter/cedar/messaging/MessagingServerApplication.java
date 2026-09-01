package org.metadatacenter.cedar.messaging;

import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.metadatacenter.cedar.messaging.resources.CommandResource;
import org.metadatacenter.cedar.messaging.resources.MessagesResource;
import org.metadatacenter.cedar.messaging.resources.SummaryResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceIndexResource;
import org.metadatacenter.cedar.util.dw.CedarHibernateBundle;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.messaging.dao.*;
import org.metadatacenter.messaging.model.*;
import org.metadatacenter.model.ServerName;
import org.metadatacenter.server.cache.user.UserSummaryCache;

public class MessagingServerApplication extends CedarMicroserviceApplication<MessagingServerConfiguration> {

  private CedarHibernateBundle<MessagingServerConfiguration> hibernate;
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
    hibernate = new CedarHibernateBundle<>(
        cedarConfig.getMessagingServerConfig(),
        PersistentMessage.class, new Class[]{
        PersistentUser.class,
        PersistentUserMessage.class,
        PersistentMessageRecipient.class,
        PersistentMessageSender.class
    }
    );
    bootstrap.addBundle(hibernate);
  }

  @Override
  public void initializeApp() {

    UserSummaryCache.init(cedarConfig, userService);

    userDAO = new PersistentUserDAO(hibernate.getSessionFactory());
    messageDAO = new PersistentMessageDAO(hibernate.getSessionFactory());
    userMessageDAO = new PersistentUserMessageDAO(hibernate.getSessionFactory());
    messageSenderDAO = new PersistentMessageSenderDAO(hibernate.getSessionFactory());
    messageRecipientDAO = new PersistentMessageRecipientDAO(hibernate.getSessionFactory());
  }

  @Override
  public void runApp(MessagingServerConfiguration configuration, Environment environment) {

    final CedarMicroserviceIndexResource index =
        new CedarMicroserviceIndexResource(cedarConfig, getServerName());
    environment.jersey().register(index);

    final MessagesResource messages = new MessagesResource(cedarConfig, userDAO, messageDAO, userMessageDAO,
        messageSenderDAO, messageRecipientDAO);
    environment.jersey().register(messages);

    final SummaryResource summary = new SummaryResource(cedarConfig, userMessageDAO);
    environment.jersey().register(summary);

    final CommandResource command = new CommandResource(cedarConfig, userMessageDAO);
    environment.jersey().register(command);

  }
}
