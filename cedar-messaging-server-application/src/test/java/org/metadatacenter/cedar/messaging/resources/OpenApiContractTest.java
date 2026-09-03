package org.metadatacenter.cedar.messaging.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

  @Test
  void messageBodiesAndResponsesAreTypedWithWireNames() throws IOException {
    JsonNode spec = readSpec();
    assertBody(spec, "/messages", "post", "application/json", "MessageRequest");
    assertBody(spec, "/messages/{id}", "patch", "application/merge-patch+json",
        "MessageNotificationPatch");

    assertResponse(spec, "/messages", "get", "MessagePage");
    assertResponse(spec, "/messages", "post", "Message");
    assertResponse(spec, "/messages/{id}", "patch", "Message");
    assertResponse(spec, "/summary", "get", "MessageSummary");
    assertResponse(spec, "/command/mark-all-as-read", "post", "UpdatedCount");

    JsonNode requestProperties = spec.at("/components/schemas/MessageRequest/properties");
    assertTrue(requestProperties.has("@id"));
    assertTrue(requestProperties.has("to"));
    assertTrue(requestProperties.has("from"));
    assertTrue(spec.at("/components/schemas/MessageNotificationPatch/properties/notificationStatus/enum")
        .toString().contains("notified"));
  }

  private static void assertBody(JsonNode spec, String path, String method, String mediaType, String schema) {
    JsonNode body = spec.path("paths").path(path).path(method).path("requestBody");
    assertTrue(body.path("required").asBoolean(), path + " " + method);
    assertEquals("#/components/schemas/" + schema,
        body.path("content").path(mediaType).path("schema").path("$ref").asText());
  }

  private static void assertResponse(JsonNode spec, String path, String method, String schema) {
    assertEquals("#/components/schemas/" + schema,
        spec.path("paths").path(path).path(method).path("responses").path("200")
            .path("content").path("application/json").path("schema").path("$ref").asText());
  }

  private static JsonNode readSpec() throws IOException {
    try (InputStream input = OpenApiContractTest.class.getResourceAsStream("/assets/swagger-api/swagger.json")) {
      assertNotNull(input, "generated OpenAPI document");
      return JsonMapper.MAPPER.readTree(input);
    }
  }
}
