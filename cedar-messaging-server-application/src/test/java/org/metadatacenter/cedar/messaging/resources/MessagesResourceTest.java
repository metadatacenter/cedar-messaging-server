package org.metadatacenter.cedar.messaging.resources;

import org.junit.*;
import org.metadatacenter.messaging.model.PersistentMessageRecipientType;
import org.metadatacenter.messaging.model.PersistentMessageSenderProcessId;
import org.metadatacenter.messaging.model.PersistentMessageSenderType;
import org.metadatacenter.util.json.JsonMapper;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

public class MessagesResourceTest extends AbstractMessagingServerResourceTest {

  @BeforeClass
  public static void oneTimeSetUp() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  @Ignore
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
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<Map<String, Object>>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
  }

  @Test
  @Ignore
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
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<Map<String, Object>>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
  }

  @Test
  @Ignore
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
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<Map<String, Object>>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
  }

}
