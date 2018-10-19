package org.metadatacenter.cedar.messaging.resources;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.metadatacenter.cedar.messaging.MessagingServerApplicationTest;
import org.metadatacenter.cedar.messaging.MessagingServerConfiguration;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.TestUserUtil;

import javax.ws.rs.client.Client;
import java.util.Map;

public abstract class AbstractMessagingServerResourceTest {

  protected static CedarConfig cedarConfig;
  protected static Client client;
  protected static String authHeader1;
  protected static String authHeader2;
  protected static String authHeaderAdmin;
  protected static final String BASE_URL = "http://localhost";
  protected static String baseUrlSummary;
  protected static String baseUrlMessages;

  @ClassRule
  public static final DropwizardAppRule<MessagingServerConfiguration> RULE =
      new DropwizardAppRule<>(MessagingServerApplicationTest.class, ResourceHelpers.resourceFilePath("test-config" +
          ".yml"));

  @BeforeClass
  public static void oneTimeSetUpAbstract() {

    SystemComponent systemComponent = SystemComponent.SERVER_MESSAGING;
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(systemComponent);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);

    AbstractMessagingServerResourceTest.cedarConfig = cedarConfig;

    baseUrlSummary = BASE_URL + ":" + RULE.getLocalPort() + "/summary";
    baseUrlMessages = BASE_URL + ":" + RULE.getLocalPort() + "/messages";

    client = new JerseyClientBuilder(RULE.getEnvironment()).build("Messaging server endpoint client");
    client.property(ClientProperties.CONNECT_TIMEOUT, 3000);
    client.property(ClientProperties.READ_TIMEOUT, 30000);

    authHeader1 = TestUserUtil.getTestUser1AuthHeader(cedarConfig);
    authHeader2 = TestUserUtil.getTestUser2AuthHeader(cedarConfig);
    authHeaderAdmin = TestUserUtil.getAdminUserAuthHeader(cedarConfig);
  }

  @Before
  public void setUpAbstract() {
  }

  @After
  public void tearDownAbstract() {
  }

}
