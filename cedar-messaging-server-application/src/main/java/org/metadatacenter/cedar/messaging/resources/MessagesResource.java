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
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.cache.user.UserSummaryCache;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.server.security.model.user.CedarUserSummary;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.*;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.constant.CedarQueryParameters.QP_NOTIFICATION_STATUS;
import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON;
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
  public Response getMessages(@QueryParam(QP_NOTIFICATION_STATUS) Optional<String> notificationStatus) throws
      CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    PersistentUserMessageNotificationStatus notificationStatusEnum = null;
    if (notificationStatus.isPresent()) {
      notificationStatusEnum = PersistentUserMessageNotificationStatus.forValue(notificationStatus.get());
      if (notificationStatusEnum == null) {
        return CedarResponse.badRequest().errorMessage("The " + QP_NOTIFICATION_STATUS + " value is invalid.")
            .parameter("notificationStatus", notificationStatus)
            .parameter("validNotificationStatus", PersistentUserMessageNotificationStatus.values())

            .build();
      }
    }

    String currentUserId = c.getCedarUser().getId();

    Map<String, Object> map = new HashMap<>();
    map.put("total", userMessageDAO.getTotalCountForUser(currentUserId));
    map.put("unread", userMessageDAO.getUnreadCountForUser(currentUserId));
    map.put("notnotified", userMessageDAO.getNotNotifiedCountForUser(currentUserId));

    List<PersistentUserMessage> list = userMessageDAO.listForUser(currentUserId, notificationStatusEnum);

    List<PersistentUserMessageExtract> messages = new ArrayList<>();

    for (PersistentUserMessage pum : list) {
      messages.add(buildUserMessageExtract(c, pum));
    }

    map.put("messages", messages);

    return Response.ok().entity(map).build();
  }

  private PersistentUserMessageExtract buildUserMessageExtract(CedarRequestContext c, PersistentUserMessage pum)
      throws CedarProcessingException {
    String screenName = null;
    if (pum.getMessage().getSender().getSenderType() == PersistentMessageSenderType.USER) {
      CedarUserSummary userSummary = UserSummaryCache.getInstance().getUser(pum.getMessage().getSender().getCid());
      screenName = userSummary.getScreenName();
    }
    return new PersistentUserMessageExtract(pum, screenName);
  }

  @POST
  @Timed
  @UnitOfWork
  public Response postMessage() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    PersistentMessageRequest message = null;
    CedarUserSummary recipient = null;

    JsonNode jsonBody = c.request().getRequestBody().asJson();
    try {
      message = JsonMapper.MAPPER.treeToValue(jsonBody, PersistentMessageRequest.class);
    } catch (JsonProcessingException e) {
      throw new CedarProcessingException(e);
    }

    PersistentMessageRecipient recipientInQuery = message.getRecipient();
    if (recipientInQuery == null) {
      return CedarResponse.badRequest().errorMessage("You need to specify a recipient").build();
    }

    PersistentMessageRecipientType recipientType = recipientInQuery.getRecipientType();
    if (recipientType == null) {
      return CedarResponse.badRequest().errorMessage("You need to specify a valid recipient type").build();
    }
    if (recipientType == PersistentMessageRecipientType.BROADCAST) {
      return CedarResponse.badRequest().errorMessage("Only the value 'user' is supported now as a recipient type")
          .build();
    }

    String recipientCid = recipientInQuery.getCid();
    recipient = UserSummaryCache.getInstance().getUser(recipientCid);
    if (recipient == null) {
      return CedarResponse.notFound().errorMessage("The specified recipient can not be found").build();
    }

    PersistentMessageRecipient persistentMessageRecipient = messageRecipientDAO.findByCid(recipient.getId());
    if (persistentMessageRecipient == null) {
      persistentMessageRecipient = new PersistentMessageRecipient();
      persistentMessageRecipient.setCid(recipient.getId());
      persistentMessageRecipient.setRecipientType(PersistentMessageRecipientType.USER);
      messageRecipientDAO.create(persistentMessageRecipient);
    }

    PersistentMessageSender persistentMessageSender = null;
    // Sender is not specified, it is the current user
    if (message.getSender() == null) {
      CedarUserSummary senderSummary = UserSummaryCache.getInstance().getUser(c.getCedarUser().getId());
      persistentMessageSender = messageSenderDAO.findByCid(senderSummary.getId());
      if (persistentMessageSender == null) {
        persistentMessageSender = new PersistentMessageSender();
        persistentMessageSender.setCid(senderSummary.getId());
        persistentMessageSender.setSenderType(PersistentMessageSenderType.USER);
        messageSenderDAO.create(persistentMessageSender);
      }
    } else {
      // Sender is specified, it must be a process
      PersistentMessageSender senderInQuery = message.getSender();
      if (senderInQuery.getSenderType() != PersistentMessageSenderType.PROCESS) {
        return CedarResponse.badRequest().errorMessage("If the sender is specified, the senderType must be 'process'")
            .build();
      } else {
        // It is a process
        PersistentMessageSenderProcessId processId = senderInQuery.getProcessId();
        if (processId == null || PersistentMessageSenderProcessId.NONE == processId) {
          return CedarResponse.badRequest().errorMessage("Unknown process id").build();
        }
        // The request must come from a user with permission
        CedarUser currentCedarUser = c.getCedarUser();
        if (!currentCedarUser.has(CedarPermission.SEND_PROCESS_MESSAGE)) {
          return CedarResponse.forbidden().errorMessage("You do not have permission to send a message in the name of " +
              "a process").build();
        }
        persistentMessageSender = messageSenderDAO.findByProcessId(processId);
        if (persistentMessageSender == null) {
          persistentMessageSender = new PersistentMessageSender();
          persistentMessageSender.setSenderType(PersistentMessageSenderType.PROCESS);
          persistentMessageSender.setProcessId(processId);
          String newSenderProcessId = linkedDataUtil.buildNewLinkedDataId(CedarNodeType.PROCESS);
          persistentMessageSender.setCid(newSenderProcessId);
          messageSenderDAO.create(persistentMessageSender);
        }
      }
    }

    PersistentUser persistentUser = userDAO.findByCid(recipient.getId());
    if (persistentUser == null) {
      persistentUser = new PersistentUser();
      persistentUser.setCid(recipient.getId());
      userDAO.create(persistentUser);
    }

    String newMessageId = linkedDataUtil.buildNewLinkedDataId(CedarNodeType.MESSAGE);
    PersistentMessage persistentMessage = new PersistentMessage();
    persistentMessage.setSubject(message.getSubject());
    persistentMessage.setBody(message.getBody());
    persistentMessage.setCid(newMessageId);
    persistentMessage.setCreationDate(ZonedDateTime.now());
    persistentMessage.setExpirationDate(null);
    persistentMessage.setSender(persistentMessageSender);
    persistentMessage.setRecipient(persistentMessageRecipient);

    String newUserMessageId = linkedDataUtil.buildNewLinkedDataId(CedarNodeType.USERMESSAGE);
    PersistentUserMessage persistentUserMessage = new PersistentUserMessage();
    persistentUserMessage.setCid(newUserMessageId);
    persistentUserMessage.setMessage(persistentMessage);
    persistentUserMessage.setUser(persistentUser);
    persistentUserMessage.setReadStatus(PersistentUserMessageReadStatus.UNREAD);
    persistentUserMessage.setNotificationStatus(PersistentUserMessageNotificationStatus.NOTNOTIFIED);

    messageDAO.create(persistentMessage);
    userMessageDAO.create(persistentUserMessage);

    PersistentUserMessageExtract pume = buildUserMessageExtract(c, persistentUserMessage);
    return Response.ok().entity(pume).build();
  }

  @PATCH
  @Timed
  @UnitOfWork
  @Path("/{id}")
  @Consumes(CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON)
  public Response patchMessage(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    PersistentUserMessage pum = userMessageDAO.findByCid(id);
    if (pum == null) {
      return CedarResponse.notFound().errorMessage("User message not found by id")
          .parameter("id", id)
          .build();
    }

    if (!c.getCedarUser().getId().equals(pum.getUser().getCid())) {
      return CedarResponse.unauthorized().errorMessage("You do not have permission to modify this user message")
          .build();
    }

    CedarParameter notificationStatus = c.request().getRequestBody().get("notificationStatus");

    String notificationStatusV = null;
    if (!notificationStatus.isEmpty()) {
      notificationStatusV = notificationStatus.stringValue();
      notificationStatusV = notificationStatusV.trim();
    }

    PersistentUserMessageNotificationStatus ns = PersistentUserMessageNotificationStatus.forValue(notificationStatusV);
    if (ns == null) {
      return CedarResponse.badRequest().errorMessage("Invalid notificationStatus").build();
    }

    pum.setNotificationStatus(ns);

    pum = userMessageDAO.update(pum);

    PersistentUserMessageExtract message = buildUserMessageExtract(c, pum);
    return Response.ok().entity(message).build();
  }

}
