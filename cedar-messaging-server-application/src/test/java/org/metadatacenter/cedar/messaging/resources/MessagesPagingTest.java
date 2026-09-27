package org.metadatacenter.cedar.messaging.resources;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.messaging.model.PersistentMessageRecipientType;
import org.metadatacenter.util.json.JsonMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GET /messages answered in CEDAR's body envelope: a page of the caller's messages beside the
 * summary counts, walked over HTTP against the in-process MariaDB the suite already runs on.
 */
public class MessagesPagingTest extends AbstractMessagingServerResourceTest {

  @BeforeEach
  public void emptyTheMessageStore() throws Exception {
    Map<String, String> env = CedarEnvironmentSource.getAll();
    String url = "jdbc:mysql://127.0.0.1:" + env.get("CEDAR_MESSAGING_MYSQL_PORT") + "/"
        + env.get("CEDAR_MESSAGING_MYSQL_DB");
    try (Connection c = DriverManager.getConnection(url, "root", ""); Statement s = c.createStatement()) {
      s.execute("DELETE FROM user_message");
      s.execute("DELETE FROM message");
    }
  }

  @Test
  public void aPageCarriesTheEnvelopeBesideTheSummaryCounts() throws Exception {
    sendToUser2(7);

    JsonNode page = list("?limit=3");

    assertEquals(3, page.get("messages").size());
    assertEquals(7, page.get("totalCount").asLong());
    assertEquals(7, page.get("total").asLong());
    assertEquals(7, page.get("unread").asLong());
    assertEquals(7, page.get("notnotified").asLong());
    assertEquals(0, page.get("currentOffset").asLong());
    assertEquals(3, page.get("request").get("limit").asInt());
    assertEquals(0, page.get("request").get("offset").asInt());
    assertTrue(page.get("paging").has("next"));
    assertEquals("6", param(page.get("paging").get("last").asText(), "offset"));
    assertFalse(page.get("paging").has("prev"));
    assertFalse(page.has("countCapped"));
  }

  @Test
  public void withoutALimitAPageHoldsOneHundredMessages() throws Exception {
    sendToUser2(105);

    JsonNode page = list("");

    assertEquals(100, page.get("messages").size());
    assertEquals(105, page.get("totalCount").asLong());
    assertEquals(5, list("?offset=100").get("messages").size());
  }

  @Test
  public void followingNextVisitsEveryMessageOnceNewestFirst() throws Exception {
    // Sent back to back, many share a creation instant at the store's precision, so only the
    // identity tie-break keeps consecutive pages disjoint.
    sendToUser2(25);

    List<JsonNode> messages = walk("?limit=4");

    assertEquals(25, messages.size());
    Set<String> ids = new HashSet<>();
    for (JsonNode message : messages) {
      assertTrue(ids.add(message.get("id").asText()), "message seen twice: " + message);
    }
    for (int i = 1; i < messages.size(); i++) {
      Instant previous = OffsetDateTime.parse(messages.get(i - 1).get("creationDate").asText()).toInstant();
      Instant current = OffsetDateTime.parse(messages.get(i).get("creationDate").asText()).toInstant();
      assertFalse(current.isAfter(previous), "messages are not newest first at " + i);
    }
  }

  @Test
  public void aNotificationFilterPagesOnlyWhatItAdmitsAndTravelsInTheLinks() throws Exception {
    sendToUser2(6);
    List<JsonNode> all = walk("?limit=10");
    for (int i = 0; i < 2; i++) {
      markNotified(all.get(i).get("id").asText());
    }

    JsonNode page = list("?notification_status=notnotified&limit=3");

    assertEquals(4, page.get("totalCount").asLong());
    assertEquals(6, page.get("total").asLong(), "total still counts every message, as the summary does");
    assertEquals(4, page.get("notnotified").asLong());
    String next = page.get("paging").get("next").asText();
    assertEquals("notnotified", param(next, "notification_status"));
    assertEquals(1, get(pathOf(next)).get("messages").size());
  }

  @Test
  public void anOffsetPastTheEndAnswersAnEmptyPage() throws Exception {
    sendToUser2(2);

    JsonNode page = list("?offset=10");

    assertEquals(0, page.get("messages").size());
    assertEquals(2, page.get("totalCount").asLong());
    assertFalse(page.get("paging").has("next"));
  }

  @Test
  public void outOfRangePagingIsRefused() {
    assertEquals(400, status("?limit=0"));
    assertEquals(400, status("?limit=501"));
    assertEquals(400, status("?offset=-1"));
    assertEquals(200, status("?limit=500"));
  }

  @Test
  public void aCallerSeesOnlyTheirOwnMessages() throws Exception {
    sendToUser2(3);

    JsonNode page = client.target(baseUrlMessages).request().header("Authorization", authHeader1)
        .get(JsonNode.class);

    assertEquals(0, page.get("totalCount").asLong());
    assertEquals(0, page.get("messages").size());
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private static void sendToUser2(int n) {
    for (int i = 0; i < n; i++) {
      Map<String, Object> content = new HashMap<>();
      content.put("subject", "Paging " + i);
      content.put("body", "Message " + i);
      Map<String, Object> to = new HashMap<>();
      to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
      to.put("@id", cedarConfig.getTestUsers().getTestUser2().getId());
      content.put("to", to);
      Response response = client.target(baseUrlMessages).request().header("Authorization", authHeaderAdmin)
          .post(Entity.entity(content, MediaType.APPLICATION_JSON));
      assertEquals(200, response.getStatus(), response.readEntity(String.class));
    }
  }

  private static void markNotified(String id) {
    Response response = client.target(baseUrlMessages + "/" + java.net.URLEncoder.encode(id, StandardCharsets.UTF_8))
        .request().header("Authorization", authHeader2)
        .method("PATCH", Entity.entity(Map.of("notificationStatus", "notified"), CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON));
    assertEquals(200, response.getStatus(), response.readEntity(String.class));
  }

  private static JsonNode list(String query) throws Exception {
    return get("/messages" + query);
  }

  private static JsonNode get(String pathAndQuery) throws Exception {
    Response response = client.target(serverRoot() + pathAndQuery).request().header("Authorization", authHeader2)
        .get();
    String body = response.readEntity(String.class);
    assertEquals(200, response.getStatus(), pathAndQuery + " -> " + body);
    return JsonMapper.STRICT_MAPPER.readTree(body);
  }

  private static int status(String query) {
    return client.target(baseUrlMessages + query).request().header("Authorization", authHeader2).get()
        .getStatus();
  }

  private static List<JsonNode> walk(String query) throws Exception {
    List<JsonNode> out = new ArrayList<>();
    String next = "/messages" + query;
    int pages = 0;
    while (next != null) {
      JsonNode page = get(next);
      page.get("messages").forEach(out::add);
      next = page.get("paging").has("next") ? pathOf(page.get("paging").get("next").asText()) : null;
      assertTrue(++pages < 1_000, "the walk did not end");
    }
    return out;
  }

  private static String serverRoot() {
    return baseUrlMessages.substring(0, baseUrlMessages.length() - "/messages".length());
  }

  private static String pathOf(String link) {
    URI uri = URI.create(link);
    assertEquals(SERVER.getLocalPort(), uri.getPort(), "links point back at the server that served them");
    return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
  }

  private static String param(String link, String name) {
    for (String pair : URI.create(link).getRawQuery().split("&")) {
      String[] kv = pair.split("=", 2);
      if (kv[0].equals(name)) {
        return URLDecoder.decode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
