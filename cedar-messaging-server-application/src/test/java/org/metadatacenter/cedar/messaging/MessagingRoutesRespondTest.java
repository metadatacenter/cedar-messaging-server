package org.metadatacenter.cedar.messaging;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceIndexResource;
import org.metadatacenter.util.test.EmbeddedCedarMySql;
import org.metadatacenter.util.test.RouteSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Route safety net: probes every endpoint of every resource the server registers, unauthenticated,
 * and requires each to answer 401. That includes the server report, health check and insight
 * resources the shared bootstrap registers in every service. A 404/405 means the route vanished or
 * changed verb; any other status means an endpoint lost its authentication assertion.
 *
 * <p>The rest of the messaging suite sends authenticated requests and asserts what comes back, so
 * nothing there would notice a gate disappearing. Messages are private correspondence between
 * users, which is what makes the gate worth pinning separately.
 *
 * <p>{@code IndexResource} is deliberately outside this surface: it serves the server's public
 * index and asserts no login.
 */
@Tag("integration")
public class MessagingRoutesRespondTest {

  static {
    // Must run before the test support boots the server, which reads the MySQL and port env vars.
    // The message store comes from an in-process MariaDB; Redis is redirected to a dead port, since
    // queue writes are best-effort and no probe here gets far enough to need one. Server ports are OS-assigned
    // from the dev server and from every other booting test class.
    EmbeddedCedarMySql.startAndRedirectEnvironment("CEDAR_MESSAGING_MYSQL", Map.of(
        "CEDAR_MESSAGING_HTTP_PORT", "0",
        "CEDAR_MESSAGING_ADMIN_PORT", "0",
        "CEDAR_MESSAGING_STOP_PORT", "0",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  private static final DropwizardTestSupport<MessagingServerConfiguration> SERVER =
      new DropwizardTestSupport<>(MessagingServerApplication.class,
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
  public void everyRouteRejectsAnUnauthenticatedRequest() {
    List<Class<?>> resources = registeredResourceClasses();
    Assertions.assertTrue(resources.size() > 1, "No resource classes were found on the booted application: "
        + resources);
    RouteSurface.assertEveryRouteAnswers(
        "http://localhost:" + SERVER.getLocalPort(),
        RouteSurface.endpoints(resources),
        401);
  }

  /**
   * Every resource class the booted application registers, its own and those the shared bootstrap
   * adds, read from Jersey rather than listed by hand. The index resource is left out: it serves the
   * server's public index and asserts no login.
   */
  private static List<Class<?>> registeredResourceClasses() {
    ResourceConfig resourceConfig = SERVER.getEnvironment().jersey().getResourceConfig();
    List<Object> registeredComponents = new ArrayList<>();
    registeredComponents.addAll(resourceConfig.getInstances());
    registeredComponents.addAll(resourceConfig.getSingletons());
    registeredComponents.addAll(resourceConfig.getClasses());
    registeredComponents.addAll(resourceConfig.getResources());
    return RouteSurface.registeredResourceClasses(registeredComponents, "org.metadatacenter").stream()
        .filter(resourceClass -> !CedarMicroserviceIndexResource.class.isAssignableFrom(resourceClass))
        .toList();
  }

}
