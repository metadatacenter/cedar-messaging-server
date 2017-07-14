package org.metadatacenter.cedar.messaging;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.HibernateConfig;

import java.util.Map;

public class CedarMessagingHibernateBundle extends HibernateBundle<MessagingServerConfiguration> {

  private final CedarConfig cedarConfig;

  protected CedarMessagingHibernateBundle(CedarConfig cedarConfig, Class<?> entity, Class<?>[] entities) {
    super(entity, entities);
    this.cedarConfig = cedarConfig;
  }

  @Override
  public PooledDataSourceFactory getDataSourceFactory(MessagingServerConfiguration messagingServerConfiguration) {
    HibernateConfig messagingServerConfig = cedarConfig.getMessagingServerConfig();
    DataSourceFactory database = new DataSourceFactory();
    database.setUrl(messagingServerConfig.getUrl());
    database.setUser(messagingServerConfig.getUser());
    database.setPassword(messagingServerConfig.getPassword());
    database.setDriverClass(messagingServerConfig.getDriverClass());
    Map<String, String> properties = database.getProperties();
    properties.putAll(messagingServerConfig.getProperties());
    return database;
  }
}
