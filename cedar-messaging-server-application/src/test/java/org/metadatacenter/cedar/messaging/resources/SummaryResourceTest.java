package org.metadatacenter.cedar.messaging.resources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.messaging.model.PersistentMessageRecipientType;
import org.metadatacenter.util.json.JsonMapper;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON;

public class SummaryResourceTest extends AbstractMessagingServerResourceTest {

  @BeforeAll
  public static void oneTimeSetUp() {
  }

  @BeforeEach
  public void setUp() {
  }

  @AfterEach
  public void tearDown() {
  }

  @Test
  public void checkSummaryForUser1() {
    String url = baseUrlSummary;
    Response response = client.target(url).request().header("Authorization", authHeader1).get();
    checkUserSummary(response);
  }

  @Test
  public void checkSummaryForUser2() {
    String url = baseUrlSummary;
    Response response = client.target(url).request().header("Authorization", authHeader2).get();
    checkUserSummary(response);
  }

  @Test
  public void checkSummaryForCedarAdmin() {
    String url = baseUrlSummary;
    Response response = client.target(url).request().header("Authorization", authHeaderAdmin).get();
    checkUserSummary(response);
  }

  /**
   * The summary moved under /messages. The top-level path it used to answer is gone.
   */
  @Test
  public void theTopLevelSummaryPathIsGone() {
    String url = BASE_URL + ":" + SERVER.getLocalPort() + "/summary";
    Response response = client.target(url).request().header("Authorization", authHeader1).get();
    Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  /**
   * The literal summary segment sits beside the message identifier segment. A message must still be
   * reached by its identifier, so a patch of a real message returns that message rather than being
   * routed to the summary.
   */
  @Test
  public void theSummarySegmentDoesNotShadowAMessage() {
    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
    to.put("@id", cedarConfig.getTestUsers().getTestUser2().getId());
    Map<String, Object> content = new HashMap<>();
    content.put("subject", "Test message reached beside the summary");
    content.put("body", "Its recipient will patch it by its identifier.");
    content.put("to", to);
    Response created = client.target(baseUrlMessages).request()
        .header("Authorization", authHeader1)
        .post(Entity.entity(content, MediaType.APPLICATION_JSON));
    Assertions.assertEquals(Status.OK.getStatusCode(), created.getStatus());
    Map<String, Object> message = created.readEntity(new GenericType<>() {
    });
    String messageId = (String) message.get("id");
    Assertions.assertNotNull(messageId, "the created message should carry an id: " + message);

    Response patched = client.target(baseUrlMessages + "/" + URLEncoder.encode(messageId, StandardCharsets.UTF_8))
        .request()
        .header("Authorization", authHeader2)
        .method("PATCH", Entity.entity(Map.of("notificationStatus", "notified"),
            CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON));
    Assertions.assertEquals(Status.OK.getStatusCode(), patched.getStatus());
    Map<String, Object> updated = patched.readEntity(new GenericType<>() {
    });
    Assertions.assertEquals(messageId, updated.get("id"));
    Assertions.assertEquals("notified", updated.get("notificationStatus"));
  }

  public void checkUserSummary(Response response) {
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<Map<String, Object>>() {
    });
    System.out.println(JsonMapper.STRICT_MAPPER.valueToTree(summary));
    Assertions.assertTrue(summary.size() == 3, "Three keys in summary");
    Assertions.assertTrue(summary.containsKey("total"), "Total is present");
    Assertions.assertTrue(summary.containsKey("unread"), "Unread is present");
    Assertions.assertTrue(summary.containsKey("notnotified"), "Notnotified is present");

  }
}
