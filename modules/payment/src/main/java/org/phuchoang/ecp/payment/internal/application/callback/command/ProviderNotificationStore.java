package org.phuchoang.ecp.payment.internal.application.callback.command;

public interface ProviderNotificationStore {
  boolean claim(String providerEventId);

  void recordUnmatched(ProviderNotification notification);
}
