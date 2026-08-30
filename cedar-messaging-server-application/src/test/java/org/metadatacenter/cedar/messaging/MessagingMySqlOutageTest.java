package org.metadatacenter.cedar.messaging;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Boots against an isolated MariaDB, then stops that database before crossing the authenticated
 * message-read boundary. A separate database process keeps this destructive fixture independent
 * from the embedded store shared by the messaging lifecycle tests.
 */
public class MessagingMySqlOutageTest {

  private static DB database;

  static {
    try {
      DBConfigurationBuilder databaseConfiguration = DBConfigurationBuilder.newBuilder();
      databaseConfiguration.setPort(0);
      database = DB.newEmbeddedDB(databaseConfiguration.build());
      database.start();

      Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
      environment.put("CEDAR_MESSAGING_MYSQL_HOST", "127.0.0.1");
      environment.put("CEDAR_MESSAGING_MYSQL_PORT", String.valueOf(database.getConfiguration().getPort()));
      environment.put("CEDAR_MESSAGING_MYSQL_USER", "root");
      environment.put("CEDAR_MESSAGING_MYSQL_PASSWORD", "");
      environment.put("CEDAR_MESSAGING_HTTP_PORT", "0");
      environment.put("CEDAR_MESSAGING_ADMIN_PORT", "0");
      environment.put("CEDAR_MESSAGING_STOP_PORT", "0");
      environment.put("CEDAR_REDIS_PERSISTENT_PORT", "1");
      CedarEnvironmentSource.setOverride(environment);
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static final DropwizardTestSupport<MessagingServerConfiguration> SERVER =
      new DropwizardTestSupport<>(MessagingServerApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"));
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  private static String authHeaderUser1;

  @BeforeAll
  public static void startServerThenDatabaseOutage() throws Exception {
    SERVER.before();
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_MESSAGING);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authHeaderUser1 = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);
    database.stop();
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
  }

  @Test
  public void messageReadReturnsSanitizedServiceUnavailable() throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + "/messages"))
        .header("Authorization", authHeaderUser1)
        .GET()
        .build();
    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    Assertions.assertEquals(503, response.statusCode(), response.body());
    JsonNode error = JsonMapper.MAPPER.readTree(response.body());
    Assertions.assertEquals("SERVICE_UNAVAILABLE", error.path("status").asText(), response.body());
    Assertions.assertEquals("SQL database is unavailable", error.path("message").asText(), response.body());
    Assertions.assertTrue(error.path("originalException").isMissingNode()
        || error.path("originalException").isNull(), response.body());
    Assertions.assertTrue(error.path("sourceException").isMissingNode()
        || error.path("sourceException").isNull(), response.body());
    Assertions.assertFalse(response.body().contains("127.0.0.1"), response.body());
  }
}
