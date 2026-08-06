package org.metadatacenter.cedar.messaging;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.messaging.resources.CommandResource;
import org.metadatacenter.cedar.messaging.resources.MessagesResource;
import org.metadatacenter.cedar.messaging.resources.SummaryResource;
import org.metadatacenter.util.test.EmbeddedCedarMySql;
import org.metadatacenter.util.test.RouteSurface;

import java.util.Map;

/**
 * Route safety net: probes every endpoint the three authenticated messaging resources declare,
 * unauthenticated, and requires each to answer 401. A 404/405 means the route vanished or changed
 * verb; any other status means an endpoint lost its authentication assertion.
 *
 * <p>The rest of the messaging suite sends authenticated requests and asserts what comes back, so
 * nothing there would notice a gate disappearing. Messages are private correspondence between
 * users, which is what makes the gate worth pinning separately.
 *
 * <p>{@code IndexResource} is deliberately outside this surface: it serves the server's public
 * index and asserts no login.
 */
public class MessagingRoutesRespondTest {

  static {
    // Must run before the test support boots the server, which reads the MySQL and port env vars.
    // The message store comes from an in-process MariaDB; Redis is redirected to a dead port, since
    // queue writes are best-effort and no probe here gets far enough to need one. Ports are distinct
    // from the dev server and from every other booting test class.
    EmbeddedCedarMySql.startAndRedirectEnvironment("CEDAR_MESSAGING_MYSQL", Map.of(
        "CEDAR_MESSAGING_HTTP_PORT", "19029",
        "CEDAR_MESSAGING_ADMIN_PORT", "19129",
        "CEDAR_MESSAGING_STOP_PORT", "19229",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  private static final DropwizardTestSupport<MessagingServerConfiguration> SERVER =
      new DropwizardTestSupport<>(MessagingServerApplicationTest.class,
          ResourceHelpers.resourceFilePath("test-config.yml"));

  @BeforeAll
  public static void startServer() throws Exception {
    SERVER.before();
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
  }

  @Test
  public void everyAuthenticatedRouteRejectsAnUnauthenticatedRequest() {
    RouteSurface.assertEveryRouteAnswers(
        "http://localhost:" + SERVER.getLocalPort(),
        RouteSurface.endpoints(
            MessagesResource.class,
            SummaryResource.class,
            CommandResource.class),
        401);
  }

}
