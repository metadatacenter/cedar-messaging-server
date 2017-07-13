package org.metadatacenter.cedar.messaging;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.messaging.health.MessagingServerHealthCheck;
import org.metadatacenter.cedar.messaging.resources.IndexResource;
import org.metadatacenter.cedar.messaging.resources.MessagesResource;
import org.metadatacenter.cedar.messaging.resources.SummaryResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.messaging.dao.PersistentMessageDAO;
import org.metadatacenter.messaging.dao.PersistentUserDAO;
import org.metadatacenter.messaging.model.PersistentMessage;
import org.metadatacenter.messaging.model.PersistentUser;
import org.metadatacenter.messaging.model.PersistentUserMessage;
import org.metadatacenter.model.ServerName;

public class MessagingServerApplication extends CedarMicroserviceApplication<MessagingServerConfiguration> {

  private final HibernateBundle<MessagingServerConfiguration> hibernate = new
      HibernateBundle<MessagingServerConfiguration>(
          PersistentUser.class,
          PersistentMessage.class,
          PersistentUserMessage.class
      ) {
        @Override
        public DataSourceFactory getDataSourceFactory(
            MessagingServerConfiguration configuration
        ) {
          return configuration.getDatabase();
        }
      };

  public static void main(String[] args) throws Exception {
    new MessagingServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.MESSAGING;
  }

  @Override
  public void initializeApp() {
  }

  @Override
  protected void initializeWithBootsrap(Bootstrap<MessagingServerConfiguration> bootstrap) {
    bootstrap.addBundle(hibernate);
  }

  @Override
  public void runApp(MessagingServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final MessagingServerHealthCheck healthCheck = new MessagingServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    final PersistentUserDAO userDAO = new PersistentUserDAO(hibernate.getSessionFactory());
    final PersistentMessageDAO messageDAO = new PersistentMessageDAO(hibernate.getSessionFactory());
    environment.jersey().register(new MessagesResource(cedarConfig, userDAO, messageDAO));
    environment.jersey().register(new SummaryResource(cedarConfig, userDAO, messageDAO));
  }
}
