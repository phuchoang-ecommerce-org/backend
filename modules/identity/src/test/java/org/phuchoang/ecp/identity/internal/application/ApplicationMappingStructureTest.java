package org.phuchoang.ecp.identity.internal.application;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.internal.application.address.AddressSummary;
import org.phuchoang.ecp.identity.internal.application.profile.AccountSummary;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** Keeps mechanical domain-to-application projection mapping in MapStruct mappers. */
class ApplicationMappingStructureTest {

    @Test
    void applicationSnapshotsDoNotContainHandwrittenDomainMappingFactories() {
        assertThat(Arrays.stream(AccountSummary.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("of"))
            .filter(method -> Modifier.isStatic(method.getModifiers())))
            .isEmpty();
        assertThat(Arrays.stream(AddressSummary.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("of"))
            .filter(method -> Modifier.isStatic(method.getModifiers())))
            .isEmpty();
    }
}
