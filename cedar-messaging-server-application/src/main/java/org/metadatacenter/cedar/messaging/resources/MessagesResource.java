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
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.cache.user.UserSummaryCache;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.server.security.model.user.CedarUserSummary;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    CedarRequestContext c = buildRequestContext();
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

  private PersistentUserMessageExtract buildUserMessageExtract(CedarRequestContext c, PersistentUserMessage pum) {
    return new PersistentUserMessageExtract(pum, senderScreenName(pum.getMessage().getSender()));
  }

  /**
   * The sender's display name, or null when there is none to be had: the sender is a process, or the
   * user server cannot answer for the id.
   * <p>
   * {@link UserSummaryCache#getUser} answers null for an id it could not resolve — a deleted account,
   * or the user server being unreachable — and that answer was dereferenced. Since a listing builds
   * one of these per message, a single unresolvable sender failed the whole of GET /messages with a
   * 500. A missing name degrades to no name, as the recipient lookup in postMessage already assumed
   * and as the resource server does for its provenance names.
   * <p>
   * Package-private so a test can ask it about a sender the cache has never held.
   */
  static String senderScreenName(PersistentMessageSender sender) {
    if (sender == null || sender.getSenderType() != PersistentMessageSenderType.USER) {
      return null;
    }
    CedarUserSummary userSummary = UserSummaryCache.getInstance().getUser(sender.getCid());
    return userSummary == null ? null : userSummary.getScreenName();
  }

  @POST
  @Timed
  @UnitOfWork
  public Response postMessage() throws CedarException {
    CedarRequestContext c = buildRequestContext();
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
      // The sender is the caller, so their id is already in hand. Asking the cache for it returned a
      // summary whose id is the id that was passed in, and dereferencing that answer failed the send
      // outright whenever the user server could not be reached — for a value the request already had.
      String senderCid = c.getCedarUser().getId();
      persistentMessageSender = messageSenderDAO.findByCid(senderCid);
      if (persistentMessageSender == null) {
        persistentMessageSender = new PersistentMessageSender();
        persistentMessageSender.setCid(senderCid);
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
          String newSenderProcessId = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.PROCESS);
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

    String newMessageId = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.MESSAGE);
    PersistentMessage persistentMessage = new PersistentMessage();
    persistentMessage.setSubject(message.getSubject());
    persistentMessage.setBody(message.getBody());
    persistentMessage.setCid(newMessageId);
    persistentMessage.setCreationDate(ZonedDateTime.now());
    persistentMessage.setExpirationDate(null);
    persistentMessage.setSender(persistentMessageSender);
    persistentMessage.setRecipient(persistentMessageRecipient);

    String newUserMessageId = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.USERMESSAGE);
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
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    PersistentUserMessage pum = userMessageDAO.findByCid(id);
    if (pum == null) {
      return CedarResponse.notFound().errorMessage("User message not found by id")
          .parameter("id", id)
          .build();
    }

    if (!c.getCedarUser().getId().equals(pum.getUser().getCid())) {
      // Forbidden, not unauthorized: the caller is identified and simply does not own this message.
      // A 401 tells them to authenticate again, which cannot help and hides the real answer.
      return CedarResponse.forbidden().errorMessage("You do not have permission to modify this user message")
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
