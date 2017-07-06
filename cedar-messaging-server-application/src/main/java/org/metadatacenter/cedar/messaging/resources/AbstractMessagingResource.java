package org.metadatacenter.cedar.messaging.resources;

import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;

public abstract class AbstractMessagingResource extends CedarMicroserviceResource {

  public AbstractMessagingResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }
}
