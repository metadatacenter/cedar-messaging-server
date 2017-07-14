package org.metadatacenter.cedar.messaging.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariable;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.TestUtil;

import java.util.HashMap;
import java.util.Map;

public class CedarConfigMessagingTest {

  @Before
  public void setEnvironment() {
    Map<String, String> env = new HashMap<>();

    env.put(CedarEnvironmentVariable.CEDAR_HOST.getName(), "metadatacenter.orgx");

    env.put(CedarEnvironmentVariable.CEDAR_NET_GATEWAY.getName(), "127.0.0.1");

    env.put(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_NAME.getName(), "cedarUser");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_PASSWORD.getName(), "password");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_HOST.getName(), "localhost");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_PORT.getName(), "27017");

    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_HOST.getName(), "127.0.0.1");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_PORT.getName(), "3306");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_DB.getName(), "cedar_messaging");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_USER.getName(), "cedar_messaging_user");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_PASSWORD.getName(), "cedar_messaging_password");

    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_HTTP_PORT.getName(), "9011");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_ADMIN_PORT.getName(), "9111");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_STOP_PORT.getName(), "9211");

    env.put(CedarEnvironmentVariable.CEDAR_USER_HTTP_PORT.getName(), "9005");

    TestUtil.setEnv(env);
  }

  @Test
  public void testGetInstance() throws Exception {
    SystemComponent systemComponent = SystemComponent.SERVER_MESSAGING;
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(systemComponent);
    CedarConfig instance = CedarConfig.getInstance(environment);
    Assert.assertNotNull(instance);
  }

}