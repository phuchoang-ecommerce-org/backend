package org.phuchoang.ecp.payment;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guards the module boundary: implementation remains private below internal. */
@AnalyzeClasses(packages = "org.phuchoang.ecp.payment", importOptions = ImportOption.DoNotIncludeTests.class)
class PaymentInternalStructureTest {

    @ArchTest
    static final ArchRule implementation_stays_under_internal = noClasses()
        .that().resideInAPackage("org.phuchoang.ecp.payment..")
        .and().resideOutsideOfPackages("org.phuchoang.ecp.payment.api..", "org.phuchoang.ecp.payment.internal..")
        .should().bePublic();
}
