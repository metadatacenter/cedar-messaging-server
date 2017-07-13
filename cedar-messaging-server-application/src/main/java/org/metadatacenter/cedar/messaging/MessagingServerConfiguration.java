package org.metadatacenter.cedar.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.DataSourceFactory;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class MessagingServerConfiguration extends CedarMicroserviceConfiguration {

  @NotNull
  @Valid
  private DataSourceFactory database = new DataSourceFactory();

  @JsonProperty("database")
  public DataSourceFactory getDatabase() {
    return database;
  }
}
