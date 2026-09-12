package org.phuchoang.ecp.catalog.domain;

import org.phuchoang.ecp.identity.api.AuthorizationService;

/**
 * A permanent, test-only fixture (never shipped in {@code catalog}'s own main sources) proving
 * {@link org.phuchoang.ecp.ArchitectureTests#identityIsReachableOnlyFromAnotherModulesApplicationPackage()}
 * actually catches a violation: a {@code catalog.domain} class naming {@code identity.api}
 * directly, which the new confinement rule forbids (`US-AUD-03`). See
 * {@code ArchitectureTests.thePlantedDomainImportOfIdentityFailsTheConfinementRule_US_AUD_03}.
 */
@SuppressWarnings("unused")
class PlantedIdentityDomainImport {

    private AuthorizationService authorizationService;
}
