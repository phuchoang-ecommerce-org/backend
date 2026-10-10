package org.phuchoang.ecp.cart.internal.application.cart;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


class ApplicationRepositoryBoundaryTest {

    @Test
    void applicationClassesDoNotDependOnTheApiPackage() {
        noClasses()
            .should().dependOnClassesThat().resideInAPackage("..cart.api..")
            .because("Cart application use cases own their inputs, outputs, and failures; the API facade adapts them for consumers")
            .check(new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.phuchoang.ecp.cart.internal.application"));
    }

    @Test
    void applicationClassesDoNotDependOnTheAggregateRepository() {
        noClasses()
            .should().dependOnClassesThat().resideInAPackage("..cart.internal.domain.repository..")
            .because("Cart commands must invoke the constraint-bearing domain model, while reads use application query ports")
            .check(new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.phuchoang.ecp.cart.internal.application"));
    }
}
