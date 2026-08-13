package org.metadatacenter.cedar.messaging.resources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.messaging.model.PersistentMessageRecipientType;
import org.metadatacenter.messaging.model.PersistentMessageSender;
import org.metadatacenter.messaging.model.PersistentMessageSenderProcessId;
import org.metadatacenter.messaging.model.PersistentMessageSenderType;
import org.metadatacenter.server.cache.user.UserSummaryCache;
import org.metadatacenter.server.security.model.user.CedarUserSummary;
import org.metadatacenter.util.json.JsonMapper;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON;

public class MessagesResourceTest extends AbstractMessagingServerResourceTest {

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
  public void sendFromUser1ToUser2() {
    String url = baseUrlMessages;
    Map<String, Object> content = new HashMap<>();
    content.put("subject", "Test message from Test User 1 to Test User 2");
    content.put("body", "Test message content\nSecond line!\n\nThe CEDAR Team");

    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
    to.put("@id", cedarConfig.getTestUsers().getTestUser2().getId());
    content.put("to", to);

    System.out.println(JsonMapper.MAPPER.valueToTree(content));

    Entity postContent = Entity.entity(content, MediaType.APPLICATION_JSON);
    Response response = client.target(url).request().header("Authorization", authHeader1).post(postContent);
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
  }

  @Test
  public void sendFromCedarAdminToUser1() {
    String url = baseUrlMessages;
    Map<String, Object> content = new HashMap<>();
    content.put("subject", "Test message from Cedar Admin to Test User 1");
    content.put("body", "Test message content\nSecond line!\n\nThe CEDAR Team");

    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
    to.put("@id", cedarConfig.getTestUsers().getTestUser1().getId());
    content.put("to", to);

    System.out.println(JsonMapper.MAPPER.valueToTree(content));

    Entity postContent = Entity.entity(content, MediaType.APPLICATION_JSON);
    Response response = client.target(url).request().header("Authorization", authHeaderAdmin).post(postContent);
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
  }

  /**
   * A sender whose display name cannot be resolved leaves the name out, rather than failing.
   *
   * <p>{@code UserSummaryCache.getUser} answers null for an id it cannot resolve — a deleted account,
   * or the user server being unreachable — and that answer was dereferenced. A listing builds one
   * extract per message, so one unresolvable sender turned the whole of GET /messages into a 500.
   *
   * <p>Driven through a sender carrying no id, which reaches the same null answer without a lookup,
   * so the suite stays free of any backend. The branch it proves is the one an unresolvable id takes.
   */
  @Test
  public void aSenderWithNoResolvableSummaryYieldsNoScreenName() {
    PersistentMessageSender sender = new PersistentMessageSender();
    sender.setSenderType(PersistentMessageSenderType.USER);
    sender.setCid(null);

    Assertions.assertNull(MessagesResource.senderScreenName(sender));
    Assertions.assertNull(MessagesResource.senderScreenName(null));
  }

  /**
   * Patching a message belonging to someone else is refused as forbidden, not unauthorized. The
   * caller is identified and simply does not own the message; a 401 tells them to authenticate
   * again, which cannot help.
   */
  @Test
  public void patchingAnotherUsersMessageIsForbidden() {
    Map<String, Object> content = new HashMap<>();
    content.put("subject", "Test message whose recipient is Test User 2");
    content.put("body", "Only its recipient may patch it.");

    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
    to.put("@id", cedarConfig.getTestUsers().getTestUser2().getId());
    content.put("to", to);

    Response created = client.target(baseUrlMessages).request()
        .header("Authorization", authHeader1)
        .post(Entity.entity(content, MediaType.APPLICATION_JSON));
    Assertions.assertEquals(Status.OK.getStatusCode(), created.getStatus());
    Map<String, Object> message = created.readEntity(new GenericType<>() {
    });
    String messageId = (String) message.get("id");
    Assertions.assertNotNull(messageId, "the created message should carry an id: " + message);

    // User 1 sent it, so it belongs to user 2. The sender is not its owner either.
    Response patched = client.target(baseUrlMessages + "/" + URLEncoder.encode(messageId, StandardCharsets.UTF_8))
        .request()
        .header("Authorization", authHeader1)
        .method("PATCH", Entity.entity(Map.of("notificationStatus", "notified"),
            CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON));
    Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), patched.getStatus());
  }

  /**
   * Concurrent first messages to the same new recipient all succeed.
   *
   * <p>A send looks for the recipient, sender and user rows and inserts whichever are missing. Two
   * sends naming a recipient for the first time both found nothing and both inserted, and the unique
   * constraints on cid failed the loser — a 500 for a request that had done nothing wrong. The rows
   * are database-generated ids, so that insert is issued as the entity is persisted rather than at
   * commit, which puts the violation inside the request and leaves the session unusable.
   *
   * <p>Aimed at a recipient no earlier test has used, so every request in the burst races on the same
   * three rows rather than finding them already there.
   */
  @Test
  public void concurrentFirstMessagesToANewRecipientAllSucceed() throws Exception {
    String newRecipientId = "https://metadatacenter.orgx/users/11111111-2222-3333-4444-999999999999";
    CedarUserSummary summary = new CedarUserSummary();
    summary.setId(newRecipientId);
    summary.setScreenName("Brand New Recipient");
    UserSummaryCache.getInstance().put(summary);

    int concurrent = 6;
    ExecutorService pool = Executors.newFixedThreadPool(concurrent);
    CountDownLatch releaseTogether = new CountDownLatch(1);
    List<Future<Integer>> statuses = new ArrayList<>();
    try {
      for (int i = 0; i < concurrent; i++) {
        int n = i;
        statuses.add(pool.submit(() -> {
          Map<String, Object> content = new HashMap<>();
          content.put("subject", "concurrent first message " + n);
          content.put("body", "body");
          Map<String, Object> to = new HashMap<>();
          to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
          to.put("@id", newRecipientId);
          content.put("to", to);

          releaseTogether.await();
          return client.target(baseUrlMessages).request()
              .header("Authorization", authHeader1)
              .post(Entity.entity(content, MediaType.APPLICATION_JSON))
              .getStatus();
        }));
      }
      releaseTogether.countDown();
      for (Future<Integer> status : statuses) {
        Assertions.assertEquals(Status.OK.getStatusCode(), status.get(60, TimeUnit.SECONDS),
            "every concurrent send to a new recipient should succeed");
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void sendFromProcessToUser2() {
    String url = baseUrlMessages;
    Map<String, Object> content = new HashMap<>();
    content.put("subject", "Test message from Process to Test User 2");
    content.put("body", "Test message content\nSecond line!\n\nThe CEDAR Team");

    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
    to.put("@id", cedarConfig.getTestUsers().getTestUser2().getId());
    content.put("to", to);

    Map<String, Object> from = new HashMap<>();
    from.put("senderType", PersistentMessageSenderType.PROCESS.getValue());
    from.put("processId", PersistentMessageSenderProcessId.SUBMISSION_NCBI.getValue());
    content.put("from", from);

    System.out.println(JsonMapper.MAPPER.valueToTree(content));

    Entity postContent = Entity.entity(content, MediaType.APPLICATION_JSON);
    Response response = client.target(url).request().header("Authorization", authHeaderAdmin).post(postContent);
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
  }

}
