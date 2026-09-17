package org.phuchoang.ecp.catalog.internal.domain;

import org.phuchoang.ecp.identity.api.authorization.AuthorizationService;

/**
 * A permanent, test-only fixture (never shipped in {@code catalog}'s own main sources) proving
 * {@code ArchitectureTests.identityIsNotNamedFromAnotherModulesDomainPackage}
 * actually catches a violation: a {@code catalog.domain} class naming {@code identity.api}
 * directly, which the new confinement rule forbids (`US-AUD-03`). See
 * {@code ArchitectureTests.thePlantedDomainImportOfIdentityFailsTheConfinementRule_US_AUD_03}.
 */
@SuppressWarnings("unused")
class PlantedIdentityDomainImport {

    private AuthorizationService authorizationService;
}
