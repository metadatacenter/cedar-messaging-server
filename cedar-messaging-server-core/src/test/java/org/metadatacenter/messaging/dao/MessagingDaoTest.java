package org.metadatacenter.messaging.dao;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.messaging.model.PersistentMessage;
import org.metadatacenter.messaging.model.PersistentMessageRecipient;
import org.metadatacenter.messaging.model.PersistentMessageRecipientType;
import org.metadatacenter.messaging.model.PersistentMessageSender;
import org.metadatacenter.messaging.model.PersistentMessageSenderProcessId;
import org.metadatacenter.messaging.model.PersistentMessageSenderType;
import org.metadatacenter.messaging.model.PersistentUser;
import org.metadatacenter.messaging.model.PersistentUserMessage;
import org.metadatacenter.messaging.model.PersistentUserMessageNotificationStatus;
import org.metadatacenter.messaging.model.PersistentUserMessageReadStatus;
import org.metadatacenter.util.test.EmbeddedCedarMySql;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.metadatacenter.messaging.model.PersistentUserMessageNotificationStatus.NOTIFIED;
import static org.metadatacenter.messaging.model.PersistentUserMessageNotificationStatus.NOTNOTIFIED;
import static org.metadatacenter.messaging.model.PersistentUserMessageReadStatus.READ;
import static org.metadatacenter.messaging.model.PersistentUserMessageReadStatus.UNREAD;

/**
 * The messaging store's DAOs against a MariaDB, with the schema Hibernate builds from the entities.
 * Each test runs in a transaction of its own and identifies its rows by identifiers no other test
 * uses, so the tests share one database.
 */
@Tag("integration")
class MessagingDaoTest {

  private static SessionFactory sessions;

  private Transaction transaction;
  private PersistentUserDAO users;
  private PersistentMessageDAO messages;
  private PersistentUserMessageDAO userMessages;
  private PersistentMessageSenderDAO senders;
  private PersistentMessageRecipientDAO recipients;

  @BeforeAll
  static void startTheStore() {
    EmbeddedCedarMySql.startAndRedirectEnvironment("CEDAR_MESSAGING_MYSQL", Map.of());
    Map<String, String> environment = CedarEnvironmentSource.getAll();
    sessions = new Configuration()
        .setProperty("jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver")
        .setProperty("jakarta.persistence.jdbc.url", "jdbc:mysql://127.0.0.1:"
            + environment.get("CEDAR_MESSAGING_MYSQL_PORT") + "/messaging_dao_test?createDatabaseIfNotExist=true")
        .setProperty("jakarta.persistence.jdbc.user", "root")
        .setProperty("jakarta.persistence.jdbc.password", "")
        .setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
        .setProperty("hibernate.hbm2ddl.auto", "create")
        .setProperty("hibernate.current_session_context_class", "thread")
        .addAnnotatedClass(PersistentUser.class)
        .addAnnotatedClass(PersistentMessage.class)
        .addAnnotatedClass(PersistentUserMessage.class)
        .addAnnotatedClass(PersistentMessageSender.class)
        .addAnnotatedClass(PersistentMessageRecipient.class)
        .buildSessionFactory();
  }

  @AfterAll
  static void closeTheStore() {
    sessions.close();
  }

  @BeforeEach
  void begin() {
    transaction = sessions.getCurrentSession().beginTransaction();
    users = new PersistentUserDAO(sessions);
    messages = new PersistentMessageDAO(sessions);
    userMessages = new PersistentUserMessageDAO(sessions);
    senders = new PersistentMessageSenderDAO(sessions);
    recipients = new PersistentMessageRecipientDAO(sessions);
  }

  @AfterEach
  void commit() {
    transaction.commit();
  }

  // Rows found or created by their identifier

  @Test
  void aUserARecipientAndAUserSenderAreEachOneRowPerIdentifier() {
    String cid = newCid();
    String other = newCid();

    assertEquals(users.findOrCreateByCid(cid).getId(), users.findOrCreateByCid(cid).getId());
    assertNotEquals(users.findOrCreateByCid(cid).getId(), users.findOrCreateByCid(other).getId());
    assertEquals(cid, users.findOrCreateByCid(cid).getCid());

    PersistentMessageRecipient recipient = recipients.findOrCreateByCid(cid, PersistentMessageRecipientType.USER);
    assertEquals(recipient.getId(), recipients.findOrCreateByCid(cid, PersistentMessageRecipientType.USER).getId());
    assertEquals(PersistentMessageRecipientType.USER, recipient.getRecipientType());
    assertNotEquals(recipient.getId(),
        recipients.findOrCreateByCid(other, PersistentMessageRecipientType.USER).getId());

    PersistentMessageSender sender = senders.findOrCreateByCid(cid, PersistentMessageSenderType.USER);
    assertEquals(sender.getId(), senders.findOrCreateByCid(cid, PersistentMessageSenderType.USER).getId());
    assertEquals(PersistentMessageSenderType.USER, sender.getSenderType());
    assertNull(sender.getProcessId());
  }

  /** A process holds one sender row, kept under the identifier it was first given, and processes hold one each. */
  @Test
  void aProcessIsOneSenderRowWhateverIdentifierALaterSendBrings() {
    PersistentMessageSender first =
        senders.findOrCreateByProcessId(PersistentMessageSenderProcessId.SUBMISSION_IMMPORT, newCid());
    PersistentMessageSender again =
        senders.findOrCreateByProcessId(PersistentMessageSenderProcessId.SUBMISSION_IMMPORT, newCid());
    PersistentMessageSender another =
        senders.findOrCreateByProcessId(PersistentMessageSenderProcessId.SUBMISSION_NCBI, newCid());

    assertEquals(first.getId(), again.getId());
    assertEquals(first.getCid(), again.getCid());
    assertEquals(PersistentMessageSenderType.PROCESS, again.getSenderType());
    assertEquals(PersistentMessageSenderProcessId.SUBMISSION_IMMPORT, again.getProcessId());
    assertNotEquals(first.getId(), another.getId());
    assertEquals(PersistentMessageSenderProcessId.SUBMISSION_NCBI, another.getProcessId());
  }

  // A user's messages

  /**
   * A listing is the user's messages alone, newest first and, within one instant, the later row first,
   * filtered by read and notification status, and paged without repeating or skipping one.
   */
  @Test
  void aUsersMessagesAreListedNewestFirstFilteredAndPaged() {
    String user = newCid();
    String someoneElse = newCid();
    ZonedDateTime now = ZonedDateTime.now().withNano(0);
    PersistentUserMessage oldest = send(user, now.minusDays(2), UNREAD, NOTNOTIFIED);
    PersistentUserMessage sameInstantFirst = send(user, now.minusDays(1), READ, NOTIFIED);
    PersistentUserMessage sameInstantSecond = send(user, now.minusDays(1), UNREAD, NOTIFIED);
    PersistentUserMessage newest = send(user, now, UNREAD, NOTNOTIFIED);
    send(someoneElse, now, UNREAD, NOTNOTIFIED);

    assertEquals(cids(newest, sameInstantSecond, sameInstantFirst, oldest),
        cids(userMessages.listForUser(user, null, null, null, 0)));
    assertEquals(cids(sameInstantSecond, sameInstantFirst), cids(userMessages.listForUser(user, null, null, 2, 1)));
    assertEquals(cids(oldest), cids(userMessages.listForUser(user, null, null, 2, 3)));
    assertEquals(cids(newest, sameInstantSecond, oldest), cids(userMessages.listForUser(user, UNREAD)));
    assertEquals(cids(newest, oldest), cids(userMessages.listForUser(user, NOTNOTIFIED)));
    assertEquals(cids(newest, oldest), cids(userMessages.listForUser(user, NOTNOTIFIED, UNREAD)));

    assertEquals(4, userMessages.countForUser(user, null, null));
    assertEquals(3, userMessages.countForUser(user, null, UNREAD));
    assertEquals(2, userMessages.countForUser(user, NOTNOTIFIED, null));
    assertEquals(4, userMessages.getTotalCountForUser(user));
    assertEquals(3, userMessages.getUnreadCountForUser(user));
    assertEquals(2, userMessages.getNotNotifiedCountForUser(user));
    assertEquals(newest.getId(), userMessages.findByCid(newest.getCid()).getId());
    assertNull(userMessages.findByCid(newCid()));
  }

  /** Marking all read changes the user's unread messages alone, and says how many it changed. */
  @Test
  void markingAllReadChangesOnlyTheUsersUnreadMessages() {
    String user = newCid();
    String someoneElse = newCid();
    ZonedDateTime now = ZonedDateTime.now();
    send(user, now, UNREAD, NOTNOTIFIED);
    send(user, now, UNREAD, NOTIFIED);
    send(user, now, READ, NOTIFIED);
    send(someoneElse, now, UNREAD, NOTNOTIFIED);

    assertEquals(2, userMessages.markAllAsRead(user));
    sessions.getCurrentSession().clear();

    assertEquals(0, userMessages.getUnreadCountForUser(user));
    assertEquals(3, userMessages.getTotalCountForUser(user));
    assertEquals(1, userMessages.getUnreadCountForUser(someoneElse));
    assertEquals(0, userMessages.markAllAsRead(user));
  }

  /** A changed notification status is what a later read of the message finds. */
  @Test
  void anUpdatedNotificationStatusIsStored() {
    PersistentUserMessage message = send(newCid(), ZonedDateTime.now(), UNREAD, NOTNOTIFIED);

    message.setNotificationStatus(NOTIFIED);
    userMessages.update(message);
    sessions.getCurrentSession().flush();
    sessions.getCurrentSession().clear();

    assertEquals(NOTIFIED, userMessages.findByCid(message.getCid()).getNotificationStatus());
  }

  private PersistentUserMessage send(String userCid, ZonedDateTime created,
                                     PersistentUserMessageReadStatus readStatus,
                                     PersistentUserMessageNotificationStatus notificationStatus) {
    PersistentMessage message = new PersistentMessage();
    message.setCid(newCid());
    message.setSubject("subject");
    message.setBody("body");
    message.setCreationDate(created);
    message.setSender(senders.findOrCreateByCid(newCid(), PersistentMessageSenderType.USER));
    message.setRecipient(recipients.findOrCreateByCid(userCid, PersistentMessageRecipientType.USER));
    messages.create(message);
    PersistentUserMessage userMessage = new PersistentUserMessage();
    userMessage.setCid(newCid());
    userMessage.setUser(users.findOrCreateByCid(userCid));
    userMessage.setMessage(message);
    userMessage.setReadStatus(readStatus);
    userMessage.setNotificationStatus(notificationStatus);
    userMessages.create(userMessage);
    return userMessage;
  }

  private static List<String> cids(PersistentUserMessage... userMessages) {
    return cids(List.of(userMessages));
  }

  private static List<String> cids(List<PersistentUserMessage> userMessages) {
    return userMessages.stream().map(PersistentUserMessage::getCid).toList();
  }

  private static String newCid() {
    return "https://repo.metadatacenter.orgx/" + UUID.randomUUID();
  }
}
