package org.metadatacenter.cedar.messaging;

import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.messaging.health.MessagingServerHealthCheck;
import org.metadatacenter.cedar.messaging.resources.IndexResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.model.ServerName;

public class MessagingServerApplication extends CedarMicroserviceApplication<MessagingServerConfiguration> {

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
  public void runApp(MessagingServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final MessagingServerHealthCheck healthCheck = new MessagingServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
