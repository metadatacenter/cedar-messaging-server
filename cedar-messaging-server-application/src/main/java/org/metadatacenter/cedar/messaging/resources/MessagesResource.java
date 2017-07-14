package org.metadatacenter.cedar.messaging.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.PATCH;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.messaging.dao.PersistentMessageDAO;
import org.metadatacenter.messaging.dao.PersistentUserDAO;
import org.metadatacenter.messaging.model.PersistentUserMessage;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
public class MessagesResource extends AbstractMessagingResource {

  private static final Logger log = LoggerFactory.getLogger(MessagesResource.class);
  private final PersistentUserDAO userDAO;
  private final PersistentMessageDAO messageDAO;

  public MessagesResource(CedarConfig cedarConfig, PersistentUserDAO userDAO, PersistentMessageDAO messageDAO) {
    super(cedarConfig);
    this.userDAO = userDAO;
    this.messageDAO = messageDAO;
  }

  @GET
  @Timed
  @UnitOfWork
  public Response getMessages() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    List<PersistentUserMessage> messages = new ArrayList<>();

    messages.add(new PersistentUserMessage());
    messages.add(new PersistentUserMessage());

    Map<String, Object> map = new HashMap<>();
    map.put("total", 10);
    map.put("unread", 4);
    map.put("messages", messages);

    return Response.ok().entity(map).build();
  }

  @POST
  @Timed
  public Response postMessage() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    Map<String, Object> map = new HashMap<>();
    map.put("messageId", 12345);

    return Response.ok().entity(map).build();
  }

  @PATCH
  @Timed
  @Path("/{id}")
  public Response patchMessage(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    Map<String, Object> map = new HashMap<>();
    map.put("messageId", 12345);
    map.put("status", "read");

    return Response.ok().entity(map).build();
  }

}
