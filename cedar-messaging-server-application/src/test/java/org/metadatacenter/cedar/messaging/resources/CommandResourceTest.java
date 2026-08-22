package org.metadatacenter.cedar.messaging.resources;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metadatacenter.messaging.model.PersistentMessageRecipientType;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

/**
 * The mark-all-as-read command, which had no test.
 *
 * <p>What it reports as {@code updated} is the count the database answers with, not the size of a
 * list read before the update. The two agree when nothing else is happening, which is why the
 * assertions below pin the count against the unread total rather than against a number written into
 * the test: a count that is merely plausible would pass either way.
 */
public class CommandResourceTest extends AbstractMessagingServerResourceTest {

  private String markAllAsReadUrl() {
    return BASE_URL + ":" + SERVER.getLocalPort() + "/command/mark-all-as-read";
  }

  private void sendMessageToUser2(String subject) {
    Map<String, Object> content = new HashMap<>();
    content.put("subject", subject);
    content.put("body", "body");

    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
    to.put("@id", cedarConfig.getTestUsers().getTestUser2().getId());
    content.put("to", to);

    Response response = client.target(baseUrlMessages).request()
        .header("Authorization", authHeader1)
        .post(Entity.entity(content, MediaType.APPLICATION_JSON));
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  private long unreadCountForUser2() {
    Response response = client.target(baseUrlSummary).request()
        .header("Authorization", authHeader2)
        .get();
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Map<String, Object> summary = response.readEntity(new GenericType<>() {
    });
    return ((Number) summary.get("unread")).longValue();
  }

  private int markAllAsReadForUser2() {
    Response response = client.target(markAllAsReadUrl()).request()
        .header("Authorization", authHeader2)
        .post(Entity.entity(Map.of(), MediaType.APPLICATION_JSON));
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Map<String, Object> result = response.readEntity(new GenericType<>() {
    });
    return ((Number) result.get("updated")).intValue();
  }

  @Test
  public void theReportedCountMatchesWhatWasUnread() {
    // Start from a known state, whatever earlier tests in this class left behind.
    markAllAsReadForUser2();
    Assertions.assertEquals(0, unreadCountForUser2(), "the mailbox should be fully read to begin with");

    sendMessageToUser2("first unread");
    sendMessageToUser2("second unread");
    long unreadBefore = unreadCountForUser2();
    Assertions.assertEquals(2, unreadBefore);

    Assertions.assertEquals(unreadBefore, markAllAsReadForUser2(),
        "the command should report exactly the number of messages that were unread");
    Assertions.assertEquals(0, unreadCountForUser2(), "nothing should be left unread");
  }

  /**
   * Nothing unread means nothing updated. The bulk statement reports zero because it changed no rows,
   * which is the same answer the old count reached by finding an empty list.
   */
  @Test
  public void markingAnAlreadyReadMailboxUpdatesNothing() {
    markAllAsReadForUser2();
    Assertions.assertEquals(0, markAllAsReadForUser2());
    Assertions.assertEquals(0, unreadCountForUser2());
  }

  /**
   * One user's command leaves another user's mail alone. The statement selects on the recipient, and
   * a bulk update that lost that condition would read every mailbox in the system.
   */
  @Test
  public void markingReadDoesNotTouchAnotherUsersMail() {
    markAllAsReadForUser2();

    // A message to user 1, which user 2's command must not touch.
    Map<String, Object> content = new HashMap<>();
    content.put("subject", "for user 1 only");
    content.put("body", "body");
    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", PersistentMessageRecipientType.USER.getValue());
    to.put("@id", cedarConfig.getTestUsers().getTestUser1().getId());
    content.put("to", to);
    Response sent = client.target(baseUrlMessages).request()
        .header("Authorization", authHeader2)
        .post(Entity.entity(content, MediaType.APPLICATION_JSON));
    Assertions.assertEquals(Status.OK.getStatusCode(), sent.getStatus());

    Response beforeSummary = client.target(baseUrlSummary).request()
        .header("Authorization", authHeader1)
        .get();
    Map<String, Object> before = beforeSummary.readEntity(new GenericType<>() {
    });
    long user1UnreadBefore = ((Number) before.get("unread")).longValue();
    Assertions.assertTrue(user1UnreadBefore > 0, "user 1 should have unread mail for this to mean anything");

    Assertions.assertEquals(0, markAllAsReadForUser2(), "user 2 has nothing unread to mark");

    Response afterSummary = client.target(baseUrlSummary).request()
        .header("Authorization", authHeader1)
        .get();
    Map<String, Object> after = afterSummary.readEntity(new GenericType<>() {
    });
    Assertions.assertEquals(user1UnreadBefore, ((Number) after.get("unread")).longValue(),
        "user 1's unread mail must be untouched by user 2's command");
  }
}
