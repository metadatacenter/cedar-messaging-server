package org.metadatacenter.cedar.messaging.health;

import com.codahale.metrics.health.HealthCheck;

public class MessagingServerHealthCheck extends HealthCheck {

  public MessagingServerHealthCheck() {
  }

  @Override
  protected Result check() throws Exception {
    if (2 * 2 == 5) {
      return Result.unhealthy("Unhealthy, because 2 * 2 == 5");
    }
    return Result.healthy();
  }
}