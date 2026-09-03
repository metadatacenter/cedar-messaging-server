package org.metadatacenter.cedar.messaging.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Messages")
@SecurityRequirement(name = "api_key")
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
  @Operation(summary = "List the caller's messages",
      description = "Return the caller's messages along with the same counts the summary reports. A "
          + "sender whose display name cannot be resolved — a deleted account, or the user server "
          + "being unreachable — is returned without one rather than failing the listing.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The caller's messages, with total, unread and not-notified counts",
          content = @Content(schema = @Schema(ref = "#/components/schemas/MessagePage"))),
      @ApiResponse(responseCode = "400", description = "The notification status is not one of the accepted values"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response getMessages(
      @Parameter(description = "Return only messages in this notification state. Omit it for all of them.")
      @QueryParam(QP_NOTIFICATION_STATUS) Optional<String> notificationStatus) throws CedarException {
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
  @Operation(summary = "Send a message",
      description = "Send a message to one user. The recipient is required and must be a user: "
          + "broadcast is not supported. The sender defaults to the caller; naming a sender means "
          + "sending on behalf of a process, which needs the process-message permission.")
  @RequestBody(description = "The message and its recipient", required = true,
      content = @Content(mediaType = MediaType.APPLICATION_JSON,
          schema = @Schema(ref = "#/components/schemas/MessageRequest")))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The message as stored",
          content = @Content(schema = @Schema(ref = "#/components/schemas/Message"))),
      @ApiResponse(responseCode = "400",
          description = "No recipient, an unsupported recipient type, or a named sender that is not a known process"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Sending in a process's name without the permission to do so"),
      @ApiResponse(responseCode = "404", description = "No such recipient"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
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

    PersistentMessageRecipient persistentMessageRecipient =
        messageRecipientDAO.findOrCreateByCid(recipient.getId(), PersistentMessageRecipientType.USER);

    PersistentMessageSender persistentMessageSender = null;
    // Sender is not specified, it is the current user
    if (message.getSender() == null) {
      // The sender is the caller, so their id is already in hand. Asking the cache for it returned a
      // summary whose id is the id that was passed in, and dereferencing that answer failed the send
      // outright whenever the user server could not be reached — for a value the request already had.
      String senderCid = c.getCedarUser().getId();
      persistentMessageSender = messageSenderDAO.findOrCreateByCid(senderCid, PersistentMessageSenderType.USER);
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

    PersistentUser persistentUser = userDAO.findOrCreateByCid(recipient.getId());

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
  @Operation(summary = "Update a message's notification state",
      description = "Change the notification state of one of the caller's own messages, as a JSON "
          + "merge patch carrying `notificationStatus`. Nothing else about a message can be changed.")
  @RequestBody(description = "The new notification state", required = true,
      content = @Content(mediaType = CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON,
          schema = @Schema(ref = "#/components/schemas/MessageNotificationPatch")))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The message as updated",
          content = @Content(schema = @Schema(ref = "#/components/schemas/Message"))),
      @ApiResponse(responseCode = "400", description = "The notification status is missing or not one of the accepted values"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The message belongs to someone else"),
      @ApiResponse(responseCode = "404", description = "No such message"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response patchMessage(
      @Parameter(description = "Message identifier.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
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
