package org.metadatacenter.cedar.messaging.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.PATCH;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.messaging.dao.*;
import org.metadatacenter.messaging.model.*;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.security.model.user.CedarUserSummary;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
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
  private final PersistentUserMessageDAO userMessageDAO;
  private final PersistentMessageSenderDAO messageSenderDAO;
  private final PersistentMessageRecipientDAO messageRecipientDAO;

  public MessagesResource(CedarConfig cedarConfig, PersistentUserDAO userDAO, PersistentMessageDAO messageDAO,
                          PersistentUserMessageDAO userMessageDAO, PersistentMessageSenderDAO messageSenderDAO,
                          PersistentMessageRecipientDAO messageRecipientDAO) {
    super(cedarConfig);
    this.userDAO = userDAO;
    this.messageDAO = messageDAO;
    this.userMessageDAO = userMessageDAO;
    this.messageSenderDAO = messageSenderDAO;
    this.messageRecipientDAO = messageRecipientDAO;
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
  @UnitOfWork
  public Response postMessage() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    PersistentMessageRequest message = null;
    CedarUserSummary sender = getUserSummary(c.getCedarUser().getId(), c);
    CedarUserSummary recipient = null;

    JsonNode jsonBody = c.request().getRequestBody().asJson();
    try {
      message = JsonMapper.MAPPER.treeToValue(jsonBody, PersistentMessageRequest.class);
    } catch (JsonProcessingException e) {
      throw new CedarProcessingException(e);
    }

    if (message.getRecipient() != null) {
      String cid = message.getRecipient().getCid();
      recipient = getUserSummary(cid, c);
    }

    PersistentMessageRecipient persistentMessageRecipient = messageRecipientDAO.findByCid(recipient.getId());
    if (persistentMessageRecipient == null) {
      persistentMessageRecipient = new PersistentMessageRecipient();
      persistentMessageRecipient.setCid(recipient.getId());
      persistentMessageRecipient.setRecipientType(PersistentMessageRecipientType.USER);
      messageRecipientDAO.create(persistentMessageRecipient);
    }

    PersistentMessageSender persistentMessageSender = messageSenderDAO.findByCid(sender.getId());
    if (persistentMessageSender == null) {
      persistentMessageSender = new PersistentMessageSender();
      persistentMessageSender.setCid(sender.getId());
      persistentMessageSender.setSenderType(PersistentMessageSenderType.USER);
      messageSenderDAO.create(persistentMessageSender);
    }

    PersistentUser persistentRecipientUser = userDAO.findByCid(recipient.getId());
    if (persistentRecipientUser == null) {
      persistentRecipientUser = new PersistentUser();
      persistentRecipientUser.setCid(recipient.getId());
      userDAO.create(persistentRecipientUser);
    }

    String newMessageId = linkedDataUtil.buildNewLinkedDataId(CedarNodeType.MESSAGE);
    PersistentMessage persistentMessage = new PersistentMessage();
    persistentMessage.setSubject(message.getSubject());
    persistentMessage.setBody(message.getBody());
    persistentMessage.setCid(newMessageId);
    persistentMessage.setCreationDate(LocalDateTime.now());
    persistentMessage.setExpirationDate(null);
    persistentMessage.setSender(persistentMessageSender);

    PersistentUserMessage persistentUserMessage = new PersistentUserMessage();
    persistentUserMessage.setMessage(persistentMessage);
    persistentUserMessage.setRecipient(persistentRecipientUser);
    persistentUserMessage.setStatus(PersistentUserMessageStatus.UNREAD);

    messageDAO.create(persistentMessage);
    userMessageDAO.create(persistentUserMessage);


    Map<String, Object> map = new HashMap<>();
    map.put("message", message);
    map.put("sender", sender);
    map.put("recipient", recipient);

    map.put("persistentMessageRecipient", persistentMessageRecipient);
    map.put("persistentMessageSender", persistentMessageSender);
    map.put("persistentRecipientUser", persistentRecipientUser);
    map.put("persistentMessage", persistentMessage);
    map.put("persistentUserMessage", persistentUserMessage);

    return Response.ok().entity(map).build();
  }

  @PATCH
  @Timed
  @UnitOfWork
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
