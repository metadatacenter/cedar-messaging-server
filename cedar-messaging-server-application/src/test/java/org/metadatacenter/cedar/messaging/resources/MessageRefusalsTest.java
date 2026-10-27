package org.metadatacenter.cedar.messaging.resources;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON;

/** What a send, a listing and a notification-status change refuse, and which answer each refusal is. */
public class MessageRefusalsTest extends AbstractMessagingServerResourceTest {

  private static Map<String, Object> message(Map<String, Object> to, Map<String, Object> from) {
    Map<String, Object> content = new HashMap<>();
    content.put("subject", "A message the tests send");
    content.put("body", "Body");
    if (to != null) content.put("to", to);
    if (from != null) content.put("from", from);
    return content;
  }

  private static Map<String, Object> toUser2() {
    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", "user");
    to.put("@id", cedarConfig.getTestUsers().getTestUser2().getId());
    return to;
  }

  private static Response send(String authorization, Map<String, Object> content) {
    return client.target(baseUrlMessages).request().header("Authorization", authorization)
        .post(Entity.entity(content, MediaType.APPLICATION_JSON));
  }

  private static Response patch(String authorization, String id, Map<String, Object> body) {
    return client.target(baseUrlMessages + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)).request()
        .header("Authorization", authorization)
        .method("PATCH", Entity.entity(body, CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON));
  }

  private static void assertStatus(Status expected, Response response) {
    Assertions.assertEquals(expected.getStatusCode(), response.getStatus(), response.readEntity(String.class));
  }

  @Test
  public void aSendWithoutARecipientIsABadRequest() {
    assertStatus(Status.BAD_REQUEST, send(authHeader1, message(null, null)));
  }

  @Test
  public void aRecipientOfNoKnownTypeIsABadRequest() {
    Map<String, Object> to = toUser2();
    to.remove("recipientType");
    assertStatus(Status.BAD_REQUEST, send(authHeader1, message(to, null)));
    to.put("recipientType", "group");
    assertStatus(Status.BAD_REQUEST, send(authHeader1, message(to, null)));
  }

  @Test
  public void aBroadcastIsNotYetSupported() {
    Map<String, Object> to = toUser2();
    to.put("recipientType", "broadcast");
    assertStatus(Status.BAD_REQUEST, send(authHeader1, message(to, null)));
  }

  @Test
  public void aRecipientNobodyKnowsIsNotFound() {
    Map<String, Object> to = toUser2();
    to.put("@id", "https://metadatacenter.org/users/00000000-0000-0000-0000-000000000000");
    assertStatus(Status.NOT_FOUND, send(authHeader1, message(to, null)));
  }

  @Test
  public void aNamedSenderMustBeAProcess() {
    assertStatus(Status.BAD_REQUEST, send(authHeaderAdmin, message(toUser2(), Map.of("senderType", "user"))));
  }

  @Test
  public void aProcessSenderMustNameAKnownProcess() {
    assertStatus(Status.BAD_REQUEST, send(authHeaderAdmin, message(toUser2(), Map.of("senderType", "process"))));
    assertStatus(Status.BAD_REQUEST, send(authHeaderAdmin,
        message(toUser2(), Map.of("senderType", "process", "processId", "submission.UNKNOWN"))));
  }

  @Test
  public void sendingInAProcesssNameNeedsThePermission() {
    assertStatus(Status.FORBIDDEN, send(authHeader1,
        message(toUser2(), Map.of("senderType", "process", "processId", "submission.IMMPORT"))));
  }

  @Test
  public void aProcessSendsUnderOneSenderHoweverOftenItSends() {
    Map<String, Object> from = Map.of("senderType", "process", "processId", "submission.IMMPORT");
    assertStatus(Status.OK, send(authHeaderAdmin, message(toUser2(), from)));
    assertStatus(Status.OK, send(authHeaderAdmin, message(toUser2(), from)));
  }

  @Test
  public void aListingByAnUnknownNotificationStatusIsABadRequest() {
    Response response = client.target(baseUrlMessages).queryParam("notification_status", "seen").request()
        .header("Authorization", authHeader1).get();
    assertStatus(Status.BAD_REQUEST, response);
    Response notified = client.target(baseUrlMessages).queryParam("notification_status", "notified").request()
        .header("Authorization", authHeader1).get();
    assertStatus(Status.OK, notified);
  }

  @Test
  public void changingAMessageNobodyHasIsNotFound() {
    assertStatus(Status.NOT_FOUND, patch(authHeader2, "no-such-message", Map.of("notificationStatus", "notified")));
  }

  @Test
  public void aNotificationStatusMustBeOneOfTheKnownValues() {
    Response created = send(authHeader1, message(toUser2(), null));
    Assertions.assertEquals(Status.OK.getStatusCode(), created.getStatus());
    Map<String, Object> sent = created.readEntity(new GenericType<>() {
    });
    String id = (String) sent.get("id");
    Map<String, Object> blank = new HashMap<>();
    blank.put("notificationStatus", null);
    assertStatus(Status.BAD_REQUEST, patch(authHeader2, id, blank));
    assertStatus(Status.BAD_REQUEST, patch(authHeader2, id, Map.of("notificationStatus", "seen")));
    assertStatus(Status.OK, patch(authHeader2, id, Map.of("notificationStatus", " notified ")));
  }
}
