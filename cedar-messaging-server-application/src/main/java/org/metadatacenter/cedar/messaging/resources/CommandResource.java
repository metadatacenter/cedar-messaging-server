package org.metadatacenter.cedar.messaging.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.hibernate.UnitOfWork;
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
  public Response markAllAsRead() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    int updated = userMessageDAO.markAllAsRead(c.getCedarUser().getId());

    Map<String, Object> map = new HashMap<>();
    map.put("updated", updated);
    return Response.ok().entity(map).build();
  }


}