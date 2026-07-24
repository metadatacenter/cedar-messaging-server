package org.metadatacenter.cedar.messaging;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.metadatacenter.util.test.TestUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * An in-process MariaDB for integration tests, replacing the live MySQL. Call
 * startAndRedirectEnvironment from a static initializer, before the DropwizardAppRule starts the
 * application: it boots the embedded server on a random port (so it can never collide with, or
 * write into, a real MySQL) and redirects the CEDAR messaging MySQL environment variables. The
 * database is created by the connector (createDatabaseIfNotExist) and the schema by Hibernate
 * (hbm2ddl auto-update), so no external DDL is involved.
 */
public final class EmbeddedCedarMySql {

  private static DB db;

  private EmbeddedCedarMySql() {
  }

  public static synchronized void startAndRedirectEnvironment(Map<String, String> extraEnvironment) {
    if (db == null) {
      try {
        DBConfigurationBuilder configuration = DBConfigurationBuilder.newBuilder();
        configuration.setPort(0); // 0 picks a free port
        db = DB.newEmbeddedDB(configuration.build());
        db.start();
      } catch (Exception e) {
        throw new IllegalStateException("Could not start the embedded MariaDB", e);
      }
      Map<String, String> environment = new HashMap<>(System.getenv());
      environment.put("CEDAR_MESSAGING_MYSQL_HOST", "127.0.0.1");
      environment.put("CEDAR_MESSAGING_MYSQL_PORT", String.valueOf(db.getConfiguration().getPort()));
      environment.put("CEDAR_MESSAGING_MYSQL_USER", "root");
      environment.put("CEDAR_MESSAGING_MYSQL_PASSWORD", "");
      environment.putAll(extraEnvironment);
      TestUtil.setEnv(environment);
    }
  }

}
