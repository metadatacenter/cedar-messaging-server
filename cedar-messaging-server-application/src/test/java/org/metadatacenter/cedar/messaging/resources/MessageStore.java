package org.metadatacenter.cedar.messaging.resources;

import org.metadatacenter.config.environment.CedarEnvironmentSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The suite's message store, read and cleared directly in the embedded MariaDB the server writes to. */
final class MessageStore {

  private MessageStore() {
  }

  /** The identifiers of the sender rows that hold the process, as the server stores its name. */
  static List<String> processSenders(String processId) throws SQLException {
    try (Connection connection = connect();
         PreparedStatement query = connection.prepareStatement(
             "SELECT cid FROM message_sender WHERE processId = ? ORDER BY id")) {
      query.setString(1, processId);
      List<String> cids = new ArrayList<>();
      try (ResultSet rows = query.executeQuery()) {
        while (rows.next()) {
          cids.add(rows.getString(1));
        }
      }
      return cids;
    }
  }

  /** Removes the process's sender rows and the messages sent under them, as if it had never sent. */
  static void forgetProcess(String processId) throws SQLException {
    try (Connection connection = connect()) {
      String senders = "SELECT id FROM message_sender WHERE processId = ?";
      String messages = "SELECT id FROM message WHERE message_sender IN (" + senders + ")";
      for (String statement : List.of(
          "DELETE FROM user_message WHERE user_message_message IN (SELECT id FROM (" + messages + ") sent)",
          "DELETE FROM message WHERE message_sender IN (" + senders + ")",
          "DELETE FROM message_sender WHERE processId = ?")) {
        try (PreparedStatement delete = connection.prepareStatement(statement)) {
          delete.setString(1, processId);
          delete.executeUpdate();
        }
      }
    }
  }

  private static Connection connect() throws SQLException {
    Map<String, String> environment = CedarEnvironmentSource.getAll();
    return DriverManager.getConnection("jdbc:mysql://127.0.0.1:" + environment.get("CEDAR_MESSAGING_MYSQL_PORT")
        + "/" + environment.get("CEDAR_MESSAGING_MYSQL_DB"), "root", "");
  }
}
