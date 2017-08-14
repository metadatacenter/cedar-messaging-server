package org.metadatacenter.cedar.messaging.resources;

import org.junit.*;
import org.metadatacenter.util.json.JsonMapper;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;

public class SummaryResourceTest extends AbstractMessagingServerResourceTest {

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
  public void checkSummaryForUser1() {
    String url = baseUrlSummary;
    Response response = client.target(url).request().header("Authorization", authHeader1).get();
    checkUserSummary(response);
  }

  @Test
  @Ignore
  public void checkSummaryForUser2() {
    String url = baseUrlSummary;
    Response response = client.target(url).request().header("Authorization", authHeader2).get();
    checkUserSummary(response);
  }

  @Test
  @Ignore
  public void checkSummaryForCedarAdmin() {
    String url = baseUrlSummary;
    Response response = client.target(url).request().header("Authorization", authHeaderAdmin).get();
    checkUserSummary(response);
  }

  public void checkUserSummary(Response response) {
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    Map<String, Object> summary = response.readEntity(new GenericType<Map<String, Object>>() {
    });
    System.out.println(JsonMapper.MAPPER.valueToTree(summary));
    Assert.assertTrue("Two keys in summary", summary.size() == 2);
    Assert.assertTrue("Total is present", summary.containsKey("total"));
    Assert.assertTrue("Unread is present", summary.containsKey("unread"));

  }
}
