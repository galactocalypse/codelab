package com.codelab.megalith.e2e;

import java.util.Set;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.common.policies.data.TenantInfoImpl;
import org.testcontainers.pulsar.PulsarContainer;

/** Shared tenant/namespace provisioning for the messaging E2E tests. */
final class PulsarTestSupport {

  static final String TENANT = "codelab-orders";
  static final String NAMESPACE = "codelab-orders/local";

  private PulsarTestSupport() {}

  /**
   * A fresh broker starts with only {@code public/default}. The framework resolves {@code
   * codelab-<module>/<namespace>} at producer creation, so the tenant/namespace must exist before
   * the Spring context's publishers connect. Idempotent, so it is safe to call per test class.
   */
  static void provisionModuleTenant(PulsarContainer pulsar)
      throws PulsarAdminException, PulsarClientException {
    try (PulsarAdmin admin =
        PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build()) {
      createIgnoringConflict(
          () ->
              admin
                  .tenants()
                  .createTenant(
                      TENANT,
                      TenantInfoImpl.builder().allowedClusters(Set.of("standalone")).build()));
      createIgnoringConflict(() -> admin.namespaces().createNamespace(NAMESPACE));
    }
  }

  @FunctionalInterface
  private interface AdminMutation {
    void run() throws PulsarAdminException;
  }

  private static void createIgnoringConflict(AdminMutation mutation) throws PulsarAdminException {
    try {
      mutation.run();
    } catch (PulsarAdminException.ConflictException alreadyExists) {
      // a reused broker, or a second test class, may have provisioned it already
    }
  }
}
