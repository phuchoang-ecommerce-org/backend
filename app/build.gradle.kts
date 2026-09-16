plugins {
    id("org.springframework.boot")
    java
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Boot 4's modularised autoconfiguration splits JSON support out of spring-boot-starter-webmvc
    // (same pattern as spring-boot-starter-flyway above) — needed here directly since
    // RateLimitFilter serialises a Problem body outside the DispatcherServlet's own converters.
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    // spring-boot-starter-flyway, not raw flyway-core: Boot 4's modularised
    // autoconfiguration puts FlywayAutoConfiguration in spring-boot-flyway,
    // which only this starter pulls in.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    // Identity (Sprint 3): stateless JWT bearer auth (ADR-0016, Security.md §4/§5) and the
    // two-instance Redis topology (ADR-0034, EN-WIRE-2).
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation(project(":shared-kernel"))
    implementation(project(":identity"))
    implementation(project(":catalog"))
    implementation(project(":inventory"))
    implementation(project(":cart"))
    implementation(project(":ordering"))
    implementation(project(":payment"))
    implementation(project(":shipping"))
    implementation(project(":promotion"))
    implementation(project(":review"))
    implementation(project(":notification"))
    implementation(project(":audit"))
    implementation(project(":reporting"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Boot 4's per-starter test-slice split (Technology Stack.md's Boot 4.1.1 pin) —
    // @WebMvcTest now ships with spring-boot-webmvc's own "-test" starter, not the generic
    // spring-boot-starter-test.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.jmolecules.archunit)
    testImplementation(libs.jmolecules.ddd)
    testImplementation("com.networknt:json-schema-validator:1.5.9")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val integrationTest: SourceSet = sourceSets.create("integrationTest") {
    java.srcDir("src/integrationTest/java")
    resources.srcDir("src/integrationTest/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

configurations["integrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    "integrationTestImplementation"(platform(libs.testcontainers.bom))
    "integrationTestImplementation"("org.testcontainers:junit-jupiter")
    "integrationTestImplementation"("org.testcontainers:postgresql")
    "integrationTestImplementation"(libs.postgresql)
    "integrationTestImplementation"("org.springframework.boot:spring-boot-testcontainers")
    "integrationTestImplementation"(libs.flyway.core)
    "integrationTestImplementation"(libs.flyway.postgresql)
    "integrationTestImplementation"("org.testcontainers:testcontainers")
    "integrationTestImplementation"("org.testcontainers:kafka")
    "integrationTestImplementation"("org.springframework.boot:spring-boot-resttestclient")
    "integrationTestImplementation"("org.springframework.boot:spring-boot-restclient")
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs L4-L6 Testcontainers-backed integration tests."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTestTask)
}
