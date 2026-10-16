package org.phuchoang.ecp.shipping.internal;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureBoundaryTest {

    @Test
    void applicationDoesNotDependOnShippingApi() {
        noClasses()
            .should().dependOnClassesThat().resideInAPackage("..shipping.api..")
            .because("shipping application use cases own their commands; the API facade adapts public contracts")
            .check(mainClassesIn("org.phuchoang.ecp.shipping.internal.application"));
    }

    @Test
    void infrastructureDoesNotDependOnShippingApi() {
        noClasses()
            .should().dependOnClassesThat().resideInAPackage("..shipping.api..")
            .because("shipping infrastructure implements inward-facing ports and must not use public API contracts")
            .check(mainClassesIn("org.phuchoang.ecp.shipping.internal.infrastructure"));
    }

    @Test
    void apiDoesNotDependOnShippingDomainInternals() {
        noClasses()
            .should().dependOnClassesThat().resideInAPackage("..shipping.internal.domain..")
            .because("the public facade adapts application results, not Shipping aggregates")
            .check(mainClassesIn("org.phuchoang.ecp.shipping.api"));
    }

    private static com.tngtech.archunit.core.domain.JavaClasses mainClassesIn(String packageName) {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(packageName);
    }
}
