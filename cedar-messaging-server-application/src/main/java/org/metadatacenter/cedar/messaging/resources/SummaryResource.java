package org.metadatacenter.cedar.messaging.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.hibernate.UnitOfWork;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.messaging.dao.PersistentUserMessageDAO;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/summary")
@Produces(MediaType.APPLICATION_JSON)
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
