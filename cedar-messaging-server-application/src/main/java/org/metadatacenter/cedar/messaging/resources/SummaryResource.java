package org.metadatacenter.cedar.messaging.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashBiMap;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorReasonKey;
import org.metadatacenter.exception.CedarBackendException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.messaging.dao.PersistentMessageDAO;
import org.metadatacenter.messaging.dao.PersistentUserDAO;
import org.metadatacenter.model.FolderOrResource;
import org.metadatacenter.model.folderserver.FolderServerFolder;
import org.metadatacenter.model.folderserver.FolderServerNode;
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.FolderServiceSession;
import org.metadatacenter.server.PermissionServiceSession;
import org.metadatacenter.server.neo4j.NodeLabel;
import org.metadatacenter.server.neo4j.parameter.NodeProperty;
import org.metadatacenter.server.result.BackendCallResult;
import org.metadatacenter.server.security.model.auth.CedarNodePermissions;
import org.metadatacenter.server.security.model.auth.CedarNodePermissionsRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.auth.NodePermission;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.rest.assertion.GenericAssertions.NonEmpty;

@Path("/summary")
@Produces(MediaType.APPLICATION_JSON)
public class SummaryResource extends AbstractMessagingResource {

  private static final Logger log = LoggerFactory.getLogger(SummaryResource.class);
  private final PersistentUserDAO userDAO;
  private final PersistentMessageDAO messageDAO;

  public SummaryResource(CedarConfig cedarConfig, PersistentUserDAO userDAO, PersistentMessageDAO messageDAO) {
    super(cedarConfig);
    this.userDAO = userDAO;
    this.messageDAO = messageDAO;
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response getSummary() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    Map<String, Object> map = new HashMap<>();
    map.put("total", 10);
    map.put("unread", 4);

    return Response.ok().entity(map).build();
  }
}
