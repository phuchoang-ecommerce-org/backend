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
import java.util.Set;
import java.util.stream.Collectors;

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
                .that().resideInAPackage(base + ".internal.application..")
                .should().dependOnClassesThat().resideInAnyPackage(base + ".internal.infrastructure..", base + ".api..")
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
                .that().resideInAPackage(base + ".internal.infrastructure..")
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
                .should().dependOnClassesThat().resideInAnyPackage(base + ".internal.domain..", base + ".internal.infrastructure..")
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
                "org.springframework.modulith..", "org.jmolecules.ddd.annotation..")
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
    void aggregateRootsHaveExactlyOneDomainRepository() {
        Set<String> aggregateRoots = mainClasses.stream()
            .filter(type -> type.isAnnotatedWith(AggregateRoot.class))
            .map(type -> type.getFullName())
            .collect(Collectors.toUnmodifiableSet());
        Set<String> repositories = mainClasses.stream()
            .filter(type -> type.isAnnotatedWith(Repository.class))
            .map(type -> type.getFullName())
            .collect(Collectors.toUnmodifiableSet());

        org.assertj.core.api.Assertions.assertThat(aggregateRoots).containsExactlyInAnyOrder(
            "org.phuchoang.ecp.identity.internal.domain.model.Account",
            "org.phuchoang.ecp.catalog.internal.domain.model.Product",
            "org.phuchoang.ecp.catalog.internal.domain.model.Category");
        org.assertj.core.api.Assertions.assertThat(repositories).containsExactlyInAnyOrder(
            "org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository",
            "org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository",
            "org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository");

        ArchRule locationRule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().areAnnotatedWith(Repository.class)
            .should().beInterfaces()
            .andShould().resideInAPackage("..internal.domain.repository..");
        locationRule.check(mainClasses);
    }

    @Test
    void implementationTypesStayBelowTheModuleInternalNamespace() {
        for (String module : MODULES) {
            String base = ROOT_PACKAGE + "." + module;
            ArchRule rule = noClasses()
                .that().resideInAPackage(base + "..")
                .and().resideOutsideOfPackages(base + ".api..", base + ".internal..")
                .should().bePublic()
                .because("api is the only exposed module surface; implementation belongs below internal");
            rule.check(mainClasses);
        }
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
                .that().resideInAPackage(base + ".internal.domain..")
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
                .should().dependOnClassesThat().resideInAPackage(base + ".internal.application..")
                .because("a module's application package is internal; the only reachable surface is its api "
                    + "(Module Dependency Diagram.md §7)");
            rule.check(mainClasses);
        }
    }

    private static final String[] IDENTITY_APPLICATION_SERVICES = {
        "RegisterAccountService", "VerifyEmailService", "LoginService", "LogoutService", "RenewSessionService"
    };

    @Test
    void identityIsNotNamedFromAnotherModulesDomainPackage() {
        // US-AUD-03 (Sprint 04): a domain object asking who the caller is would make
        // authorisation part of a business invariant, forbidden by Domain Model.md §5.2 — the
        // concrete violation the sprint backlog's "identity named only from application" language
        // is about. identity.api types (CallerContext, AuthorizationService) legitimately pass
        // through another module's api/infrastructure as plumbing (a facade parameter, an
        // adapter's argument) on their way to that module's application layer, which is where
        // the actual authorisation call happens — domainDoesNotNameAnotherModule already forbids
        // any other module from being named from domain, and this rule gives identity specifically
        // its own dedicated, US-AUD-03-traceable check.
        for (String module : MODULES) {
            if (module.equals("identity") || module.equals("sharedkernel")) {
                continue;
            }
            String base = ROOT_PACKAGE + "." + module;
            ArchRule rule = noClasses()
                .that().resideInAPackage(base + ".internal.domain..")
                .should().dependOnClassesThat().resideInAPackage(ROOT_PACKAGE + ".identity..")
                .because("a domain object must not name identity — authorisation is an application "
                    + "concern (Domain Model.md §5.2, ADR-0016 §4, US-AUD-03)")
                .allowEmptyShould(true);
            rule.check(mainClasses);
        }
    }

    @Test
    void thePlantedDomainImportOfIdentityFailsTheConfinementRule_US_AUD_03() {
        // Permanent evidence (not a one-time manual step) that the rule above actually catches a
        // violation: catalog.internal.domain.PlantedIdentityDomainImport (this module's test sources only
        // — never shipped in catalog's own main sources) imports identity.api.AuthorizationService
        // directly from a domain package. This test proves the rule fails on it, then discards
        // the result — the fixture's only job is to be caught.
        JavaClasses testFixtureClasses = new ClassFileImporter()
            .importPath(Paths.get("build", "classes", "java", "test"));
        ArchRule rule = noClasses()
            .that().resideInAPackage(ROOT_PACKAGE + ".catalog.internal.domain..")
            .should().dependOnClassesThat().resideInAPackage(ROOT_PACKAGE + ".identity..")
            .because("planted-violation proof for identityIsNotNamedFromAnotherModulesDomainPackage");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rule.check(testFixtureClasses))
            .isInstanceOf(AssertionError.class);
    }

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
                ROOT_PACKAGE + ".identity.internal.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data.redis..")
            .because("Redis client types are confined to redis.RedisConfig and the identity.internal.infrastructure "
                + "adapters (ADR-0034 §9 rule B4)")
            .allowEmptyShould(true);
        rule.check(mainClasses);
    }

    @Test
    void kafkaProducerApisAreConfinedToTheCompositionRoot() {
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage(ROOT_PACKAGE + ".events..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.kafka..", "org.apache.kafka..")
            .because("bounded contexts write their own outbox through shared-kernel's OutboxWriter; only app relays to Kafka")
            .allowEmptyShould(true);
        rule.check(mainClasses);
    }

    @Test
    void catalogWritesThroughTheSharedOutboxPort() {
        ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().haveSimpleName("CreateCategoryService")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.phuchoang.ecp.sharedkernel.api.event.OutboxWriter")
            .because("a module records its event through the shared port, never another module's outbox adapter");
        rule.check(mainClasses);
    }

    @Test
    void jdbcTemplateIsReservedForOutboxRelayCoordination() {
        ArchRule rule = noClasses()
            .that().doNotHaveFullyQualifiedName(ROOT_PACKAGE + ".events.OutboxRepository")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate")
            .because("ordinary SQL uses JdbcClient; JdbcTemplate is retained only for the relay's batch/locking "
                + "coordination primitives (ADR-0010 persistence-tool matrix)")
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
