package org.metadatacenter.cedar.messaging.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentUtil;
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

    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_NAME, env);
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_PASSWORD, env);
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_HOST.getName(), "localhost");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_PORT.getName(), "27017");

    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_HOST.getName(), "127.0.0.1");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_PORT.getName(), "3306");
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_DB, env);
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_USER, env);
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MESSAGING_MYSQL_PASSWORD, env);

    env.put(CedarEnvironmentVariable.CEDAR_REDIS_PERSISTENT_HOST.getName(), "127.0.0.1");
    env.put(CedarEnvironmentVariable.CEDAR_REDIS_PERSISTENT_PORT.getName(), "6379");

    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_HTTP_PORT.getName(), "9012");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_ADMIN_PORT.getName(), "9112");
    env.put(CedarEnvironmentVariable.CEDAR_MESSAGING_STOP_PORT.getName(), "9212");

    env.put(CedarEnvironmentVariable.CEDAR_USER_HTTP_PORT.getName(), "9005");

    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_ADMIN_USER_API_KEY, env);
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_TEST_USER1_ID, env);
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_TEST_USER2_ID, env);

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