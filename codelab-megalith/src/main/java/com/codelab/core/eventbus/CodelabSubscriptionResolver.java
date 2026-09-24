package com.codelab.core.eventbus;

import com.codelab.common.spring.persistence.CodelabModule;

public class CodelabSubscriptionResolver {

  public static String resolveSubscriptionName(CodelabModule module, String subscriptionName) {
    return resolveSubscriptionName(module.name(), subscriptionName);
  }

  public static String resolveSubscriptionName(String moduleName, String subscriptionName) {
    return moduleName + "." + subscriptionName;
  }
}
