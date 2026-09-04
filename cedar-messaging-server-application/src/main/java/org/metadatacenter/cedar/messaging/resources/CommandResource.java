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

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Messages")
@SecurityRequirement(name = "api_key")
public class CommandResource extends AbstractMessagingResource {

  private static final Logger log = LoggerFactory.getLogger(CommandResource.class);

  protected static final String MARK_ALL_AS_READ_COMMAND = "mark-all-as-read";

  private final PersistentUserMessageDAO userMessageDAO;

  public CommandResource(CedarConfig cedarConfig, PersistentUserMessageDAO userMessageDAO) {
    super(cedarConfig);
    this.userMessageDAO = userMessageDAO;
  }

  @POST
  @Timed
  @UnitOfWork
  @Path("/" + MARK_ALL_AS_READ_COMMAND)
  @Operation(summary = "Mark every message as read",
      description = "Mark all of the caller's unread messages read at once, and report how many that "
          + "was. Acts only on the caller's own messages.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "How many messages were marked read",
          content = @Content(schema = @Schema(ref = "#/components/schemas/UpdatedCount"))),
      @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = CedarError.class)), description = "Unauthorized"),
      @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = CedarError.class)), description = "Internal server error")
  })
  public Response markAllAsRead() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    int updated = userMessageDAO.markAllAsRead(c.getCedarUser().getId());

    Map<String, Object> map = new HashMap<>();
    map.put("updated", updated);
    return Response.ok().entity(map).build();
  }


}
