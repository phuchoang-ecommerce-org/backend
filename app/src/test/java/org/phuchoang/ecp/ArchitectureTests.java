package org.phuchoang.ecp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The ArchUnit half of ADR-0018's governance gate — the layer and forbidden-edge rules of
 * {@code Module Dependency Diagram.md} §6-§7. {@code ApplicationModules.verify()} in
 * {@link ModularityTests} covers the module-boundary and acyclicity half.
 */
class ArchitectureTests {

    private static final String ROOT_PACKAGE = "org.phuchoang.ecp";

    private static final String[] MODULES = {
        "sharedkernel", "identity", "catalog", "inventory", "cart", "ordering",
        "payment", "shipping", "promotion", "review", "notification", "audit", "reporting"
    };

    private static JavaClasses mainClasses;

    @BeforeAll
    static void importMainClasses() {
        mainClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT_PACKAGE);
    }

    @Test
    void domainDependsOnNothingButTheSharedKernel() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "com.fasterxml.jackson..",
                "org.hibernate..")
            .because("domain depends on the shared kernel only — no Spring, JPA, Jackson, or a provider SDK "
                + "(Module Dependency Diagram.md §6)")
            .allowEmptyShould(true);
        rule.check(mainClasses);
    }

    @Test
    void applicationDoesNotDependOnItsOwnInfrastructureOrApi() {
        for (String module : MODULES) {
            String base = ROOT_PACKAGE + "." + module;
            ArchRule rule = noClasses()
                .that().resideInAPackage(base + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(base + ".infrastructure..", base + ".api..")
                .because("application may depend on domain and ports only, never its own module's "
                    + "infrastructure or api (Module Dependency Diagram.md §6)")
                .allowEmptyShould(true);
            rule.check(mainClasses);
        }
    }

    @Test
    void infrastructureDoesNotDependOnItsOwnApi() {
        for (String module : MODULES) {
            String base = ROOT_PACKAGE + "." + module;
            ArchRule rule = noClasses()
                .that().resideInAPackage(base + ".infrastructure..")
                .should().dependOnClassesThat().resideInAPackage(base + ".api..")
                .because("infrastructure may depend on domain and application, never its own module's api "
                    + "(Module Dependency Diagram.md §6)")
                .allowEmptyShould(true);
            rule.check(mainClasses);
        }
    }

    @Test
    void apiDoesNotDependOnDomainOrInfrastructureInternals() {
        for (String module : MODULES) {
            String base = ROOT_PACKAGE + "." + module;
            ArchRule rule = noClasses()
                .that().resideInAPackage(base + ".api..")
                .should().dependOnClassesThat().resideInAnyPackage(base + ".domain..", base + ".infrastructure..")
                .because("api may depend on application only, never domain internals or infrastructure "
                    + "(Module Dependency Diagram.md §6)");
            rule.check(mainClasses);
        }
    }

    @Test
    void sharedKernelHasNoOutboundDependencies() {
        ArchRule rule = noClasses()
            .that().resideInAPackage(ROOT_PACKAGE + ".sharedkernel..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                ROOT_PACKAGE + ".sharedkernel..", "java..", "javax..", "jakarta.annotation..", "lombok..",
                "org.springframework.modulith..")
            .because("shared-kernel must sit structurally beneath every module — zero outbound dependencies "
                + "on a bounded context. Spring Modulith's own annotations (@ApplicationModule, "
                + "@NamedInterface) are structural package metadata, not a context dependency "
                + "(Module Dependency Diagram.md §7)");
        rule.check(mainClasses);
    }

    @Test
    void sharedKernelCarriesNoAggregateOrRepositoryStereotype() {
        ArchRule rule = noClasses()
            .that().resideInAPackage(ROOT_PACKAGE + ".sharedkernel..")
            .should().beAnnotatedWith(AggregateRoot.class)
            .orShould().beAnnotatedWith(Repository.class)
            .because("the kernel is behaviour-only value objects; growth is the coupling trap it invites "
                + "(Module Dependency Diagram.md §7)");
        rule.check(mainClasses);
    }

    @Test
    void domainDoesNotNameAnotherModule() {
        for (String module : MODULES) {
            if (module.equals("sharedkernel")) {
                continue;
            }
            String base = ROOT_PACKAGE + "." + module;
            String[] otherModules = otherModulePackages(module);
            ArchRule rule = noClasses()
                .that().resideInAPackage(base + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(otherModules)
                .because("a domain package must not name identity or any other module — authorisation and "
                    + "cross-context coordination are application concerns (Module Dependency Diagram.md §6-7)")
                .allowEmptyShould(true);
            rule.check(mainClasses);
        }
    }

    @Test
    void noClassReachesIntoAnotherModulesApplicationPackage() {
        for (String module : MODULES) {
            String base = ROOT_PACKAGE + "." + module;
            ArchRule rule = noClasses()
                .that().resideOutsideOfPackage(base + "..")
                .should().dependOnClassesThat().resideInAPackage(base + ".application..")
                .because("a module's application package is internal; the only reachable surface is its api "
                    + "(Module Dependency Diagram.md §7)");
            rule.check(mainClasses);
        }
    }

    private static final String[] IDENTITY_APPLICATION_SERVICES = {
        "RegisterAccountService", "VerifyEmailService", "LoginService", "LogoutService"
    };

    @Test
    void everyIdentityApplicationServiceDependsOnAuthorizationService() {
        // ADR-0016 §5: "the most dangerous defect in this codebase is an @ApplicationService
        // that forgets to call AuthorizationService" — this only checks that each of the four
        // use-case services is *wired to* the OHS (a real ArchUnit call-graph check is out of
        // scope this sprint); PermissionMatrixAuthorizationService itself and the
        // IdentityApplicationService aggregator are deliberately excluded from this rule.
        ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().haveSimpleName(IDENTITY_APPLICATION_SERVICES[0])
            .or().haveSimpleName(IDENTITY_APPLICATION_SERVICES[1])
            .or().haveSimpleName(IDENTITY_APPLICATION_SERVICES[2])
            .or().haveSimpleName(IDENTITY_APPLICATION_SERVICES[3])
            .should().dependOnClassesThat().haveSimpleName("AuthorizationService")
            .because("ADR-0016 §4 — every application service calls the AuthorizationService OHS "
                + "before executing a command");
        rule.check(mainClasses);
    }

    @Test
    void onlyTheDeclaredRedisAdaptersTouchARedisClientType() {
        // ADR-0034 §9 rule B4, narrowed to what ArchUnit can check structurally: Redis access is
        // centralised in the connection-factory config and the two declared adapters, never
        // ad-hoc elsewhere — the actual key-prefix-to-factory mapping is enforced by code review.
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackages(
                ROOT_PACKAGE + ".redis..",
                ROOT_PACKAGE + ".identity.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data.redis..")
            .because("Redis client types are confined to redis.RedisConfig and the identity.infrastructure "
                + "adapters (ADR-0034 §9 rule B4)")
            .allowEmptyShould(true);
        rule.check(mainClasses);
    }

    @Test
    void fastSuiteImportsNoTestcontainers() {
        JavaClasses testClasses = new ClassFileImporter()
            .importPath(Paths.get("build", "classes", "java", "test"));
        ArchRule rule = noClasses()
            .should().dependOnClassesThat().resideInAPackage("org.testcontainers..")
            .because("a Testcontainers import in the `test` source set (the fast suite) erodes the fast/slow "
                + "split within a month (Testing and Benchmark Strategy.md §4)");
        rule.check(testClasses);
    }

    private static String[] otherModulePackages(String module) {
        return Arrays.stream(MODULES)
            .filter(m -> !m.equals(module) && !m.equals("sharedkernel"))
            .map(m -> ROOT_PACKAGE + "." + m + "..")
            .toArray(String[]::new);
    }
}
