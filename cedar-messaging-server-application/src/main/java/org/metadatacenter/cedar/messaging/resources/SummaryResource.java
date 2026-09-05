package org.metadatacenter.cedar.messaging.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.dropwizard.hibernate.UnitOfWork;
import org.metadatacenter.util.http.CedarError;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.messaging.dao.PersistentUserMessageDAO;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/summary")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Messages")
@SecurityRequirement(name = "api_key")
public class SummaryResource extends AbstractMessagingResource {

  private static final Logger log = LoggerFactory.getLogger(SummaryResource.class);
  private final PersistentUserMessageDAO userMessageDAO;

  public SummaryResource(CedarConfig cedarConfig, PersistentUserMessageDAO userMessageDAO) {
    super(cedarConfig);
    this.userMessageDAO = userMessageDAO;
  }

  @GET
  @Timed
  @UnitOfWork
  @Operation(summary = "Count the caller's messages",
      description = "Report how many messages the caller has in total, how many are unread, and how "
          + "many have not yet been notified. This is the counts alone, for a workbench badge that "
          + "does not want the messages themselves.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Total, unread, and not-yet-notified counts",
          content = @Content(schema = @Schema(ref = "#/components/schemas/MessageSummary"))),
      @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = CedarError.class)), description = "Unauthorized"),
      @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = CedarError.class)), description = "Internal server error")
  })
  public Response getSummary() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    Map<String, Object> map = new HashMap<>();
    map.put("total", userMessageDAO.getTotalCountForUser(c.getCedarUser().getId()));
    map.put("unread", userMessageDAO.getUnreadCountForUser(c.getCedarUser().getId()));
    map.put("notnotified", userMessageDAO.getNotNotifiedCountForUser(c.getCedarUser().getId()));

    return Response.ok().entity(map).build();
  }
}
