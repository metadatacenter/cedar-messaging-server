package org.metadatacenter.cedar.messaging.resources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
