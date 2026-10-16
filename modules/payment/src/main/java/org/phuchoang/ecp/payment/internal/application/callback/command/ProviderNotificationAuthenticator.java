package org.phuchoang.ecp.payment.internal.application.callback.command;

public interface ProviderNotificationAuthenticator {
  boolean authentic(ProviderNotification notification, String signature);
}
