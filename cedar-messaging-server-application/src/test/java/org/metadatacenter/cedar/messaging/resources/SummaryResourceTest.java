package org.metadatacenter.cedar.messaging.resources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.json.JsonMapper;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;

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

  public void checkUserSummary(Response response) {
    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<Map<String, Object>>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
    Assertions.assertTrue(summary.size() == 3, "Three keys in summary");
    Assertions.assertTrue(summary.containsKey("total"), "Total is present");
    Assertions.assertTrue(summary.containsKey("unread"), "Unread is present");
    Assertions.assertTrue(summary.containsKey("notnotified"), "Notnotified is present");

  }
}
