package org.metadatacenter.cedar.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.server.cache.user.UserSummaryCache;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.server.security.model.user.CedarUserSummary;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.EmbeddedCedarMySql;
import org.metadatacenter.util.test.TcpFaultProxy;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boots against the shared embedded MariaDB through a TCP fault proxy, sends a message, and then breaks
 * every connection through the proxy. During the outage a read and a send each answer a sanitized 503,
 * and the send stores nothing. Once the database is reachable again, reads recover without a restart
 * and find the message sent before the outage. Only this server's connections pass through the proxy,
 * so the outage leaves the database, and the messaging tests that share it, untouched.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessagingMySqlOutageTest {

  private static final String SENT_BEFORE = "Sent before the outage " + UUID.randomUUID();
  private static final String SENT_DURING = "Sent during the outage " + UUID.randomUUID();

  /** How long reads may take to recover: the pool validates idle connections every 30 seconds. */
  private static final Duration RECOVERY = Duration.ofSeconds(60);

  private static TcpFaultProxy databaseProxy;

  static {
    try {
      EmbeddedCedarMySql.startAndRedirectEnvironment("CEDAR_MESSAGING_MYSQL", Map.of());
      databaseProxy = TcpFaultProxy.start("127.0.0.1",
          Integer.parseInt(CedarEnvironmentSource.getAll().get("CEDAR_MESSAGING_MYSQL_PORT")));

      Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
      environment.put("CEDAR_MESSAGING_MYSQL_PORT", String.valueOf(databaseProxy.port()));
      environment.put("CEDAR_MESSAGING_HTTP_PORT", "0");
      environment.put("CEDAR_MESSAGING_ADMIN_PORT", "0");
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
  private static String authHeaderUser2;
  private static String user2;

  @BeforeAll
  public static void startServerThenDatabaseOutage() throws Exception {
    SERVER.before();
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_MESSAGING);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authHeaderUser1 = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);
    authHeaderUser2 = TestAuthUtil.getTestUser2AuthHeader(cedarConfig);
    user2 = cedarConfig.getTestUsers().getTestUser2().getId();
    for (CedarUser user : List.of(TestAuthUtil.getTestUser1(cedarConfig), TestAuthUtil.getTestUser2(cedarConfig))) {
      CedarUserSummary summary = new CedarUserSummary();
      summary.setId(user.getId());
      summary.setScreenName(user.getFirstName() + " " + user.getLastName());
      UserSummaryCache.getInstance().put(summary);
    }
    HttpResponse<String> sent = send(SENT_BEFORE);
    Assertions.assertEquals(200, sent.statusCode(), sent.body());
    databaseProxy.failConnections();
  }

  @AfterAll
  public static void stopServer() throws Exception {
    try {
      SERVER.after();
    } finally {
      databaseProxy.close();
    }
  }

  @Test
  @Order(1)
  public void messageReadReturnsSanitizedServiceUnavailable() throws Exception {
    HttpResponse<String> response = read(authHeaderUser1);

    assertSanitizedOutage(response);
  }

  @Test
  @Order(2)
  public void aSendDuringTheOutageReturnsSanitizedServiceUnavailable() throws Exception {
    assertSanitizedOutage(send(SENT_DURING));
  }

  /**
   * Once the database answers again, reads recover without restarting the server, and find the message
   * sent before the outage and not the one refused during it.
   */
  @Test
  @Order(3)
  public void readsRecoverOnceTheDatabaseIsBackAndTheRefusedSendStoredNothing() throws Exception {
    databaseProxy.restoreConnections();

    long deadline = System.nanoTime() + RECOVERY.toNanos();
    HttpResponse<String> response = read(authHeaderUser2);
    while (response.statusCode() != 200 && System.nanoTime() < deadline) {
      Thread.sleep(500);
      response = read(authHeaderUser2);
    }

    Assertions.assertEquals(200, response.statusCode(), response.body());
    List<String> subjects = new ArrayList<>();
    JsonMapper.STRICT_MAPPER.readTree(response.body()).path("messages")
        .forEach(message -> subjects.add(message.path("subject").asText()));
    Assertions.assertTrue(subjects.contains(SENT_BEFORE), subjects.toString());
    Assertions.assertFalse(subjects.contains(SENT_DURING), subjects.toString());
  }

  private static HttpResponse<String> read(String authorization) throws Exception {
    return CLIENT.send(HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + SERVER.getLocalPort() + "/messages"))
        .header("Authorization", authorization)
        .GET()
        .build(), HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> send(String subject) throws Exception {
    String body = JsonMapper.STRICT_MAPPER.writeValueAsString(Map.of("subject", subject, "body", "Body",
        "to", Map.of("recipientType", "user", "@id", user2)));
    return CLIENT.send(HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + SERVER.getLocalPort() + "/messages"))
        .header("Authorization", authHeaderUser1)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build(), HttpResponse.BodyHandlers.ofString());
  }

  private static void assertSanitizedOutage(HttpResponse<String> response) throws Exception {
    Assertions.assertEquals(503, response.statusCode(), response.body());
    JsonNode error = JsonMapper.STRICT_MAPPER.readTree(response.body());
    Assertions.assertEquals("SERVICE_UNAVAILABLE", error.path("status").asText(), response.body());
    Assertions.assertEquals("SQL database is unavailable", error.path("message").asText(), response.body());
    Assertions.assertTrue(error.path("originalException").isMissingNode()
        || error.path("originalException").isNull(), response.body());
    Assertions.assertTrue(error.path("sourceException").isMissingNode()
        || error.path("sourceException").isNull(), response.body());
    Assertions.assertFalse(response.body().contains("127.0.0.1"), response.body());
  }
}
