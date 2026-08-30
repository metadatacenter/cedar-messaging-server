package org.metadatacenter.cedar.messaging.resources;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.metadatacenter.util.test.EmbeddedCedarMySql;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.metadatacenter.cedar.messaging.MessagingServerApplication;
import org.metadatacenter.cedar.messaging.MessagingServerConfiguration;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.server.cache.user.UserSummaryCache;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.server.security.model.user.CedarUserSummary;
import org.metadatacenter.util.test.TestAuthUtil;

import jakarta.ws.rs.client.Client;
import java.util.Map;

public abstract class AbstractMessagingServerResourceTest {

  static {
    // Must run before the test support boots the server, which reads the MySQL env vars.
    // The message store comes from an in-process MariaDB; Redis is redirected to a dead port,
    // since queue writes are best-effort - the suite needs no live backend at all. Alternate
    // server ports, so the test instance never collides with a running dev server.
    EmbeddedCedarMySql.startAndRedirectEnvironment("CEDAR_MESSAGING_MYSQL", Map.of(
        "CEDAR_MESSAGING_HTTP_PORT", "0",
        "CEDAR_MESSAGING_ADMIN_PORT", "0",
        "CEDAR_MESSAGING_STOP_PORT", "0",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  protected static CedarConfig cedarConfig;
  protected static Client client;
  protected static String authHeader1;
  protected static String authHeader2;
  protected static String authHeaderAdmin;
  protected static final String BASE_URL = "http://localhost";
  protected static String baseUrlSummary;
  protected static String baseUrlMessages;

  public static final DropwizardTestSupport<MessagingServerConfiguration> SERVER =
      new DropwizardTestSupport<>(MessagingServerApplication.class, ResourceHelpers.resourceFilePath("test-config" +
          ".yml"));

  @BeforeAll
  public static void oneTimeSetUpAbstract() throws Exception {
    SERVER.before();

    SystemComponent systemComponent = SystemComponent.SERVER_MESSAGING;
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(systemComponent);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);

    AbstractMessagingServerResourceTest.cedarConfig = cedarConfig;

    baseUrlSummary = BASE_URL + ":" + SERVER.getLocalPort() + "/summary";
    baseUrlMessages = BASE_URL + ":" + SERVER.getLocalPort() + "/messages";

    // Dropwizard's default client config enables gzip, which leaves the client trying to gunzip a
    // response body that is not gzip-encoded, so readEntity() fails with "ZipException: Not in GZIP
    // format". The tests do not need compression; disable it.
    JerseyClientConfiguration clientConfig = new JerseyClientConfiguration();
    clientConfig.setGzipEnabled(false);
    clientConfig.setGzipEnabledForRequests(false);
    client = new JerseyClientBuilder(SERVER.getEnvironment()).using(clientConfig).build("Messaging server endpoint client");
    client.property(ClientProperties.CONNECT_TIMEOUT, 3000);
    client.property(ClientProperties.READ_TIMEOUT, 30000);

    // Replace the Neo4j-backed user service wired at application startup with an in-memory one,
    // so API-key authentication needs no live Neo4j (and no Keycloak)
    TestAuthUtil.installInMemoryUserService(cedarConfig);

    authHeader1 = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);
    authHeader2 = TestAuthUtil.getTestUser2AuthHeader(cedarConfig);
    authHeaderAdmin = TestAuthUtil.getAdminUserAuthHeader(cedarConfig);

    // Seed the user summary cache with the test users, so recipient resolution never falls
    // through to the user server
    seedUserSummary(TestAuthUtil.getTestUser1(cedarConfig));
    seedUserSummary(TestAuthUtil.getTestUser2(cedarConfig));
    seedUserSummary(TestAuthUtil.getAdminUser(cedarConfig));
  }

  @AfterAll
  public static void oneTimeTearDownAbstract() {
    SERVER.after();
  }

  private static void seedUserSummary(CedarUser user) {
    CedarUserSummary summary = new CedarUserSummary();
    summary.setId(user.getId());
    summary.setScreenName(user.getFirstName() + " " + user.getLastName());
    UserSummaryCache.getInstance().put(summary);
  }

  @BeforeEach
  public void setUpAbstract() {
  }

  @AfterEach
  public void tearDownAbstract() {
  }

}
