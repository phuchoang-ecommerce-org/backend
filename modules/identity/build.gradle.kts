plugins {
    `java-library`
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-events-api")
    implementation(libs.jmolecules.ddd)
    implementation(libs.jmolecules.events)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    // identity.infrastructure: JPA persistence adapters, Argon2 password hashing, JWT issuance
    // (JwtEncoder/JwtDecoder beans are configured in :app, per ADR-0034/Security.md §5.5), and the
    // Redis-backed RateLimiter/CacheAside adapters (EN-WIRE-2).
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.micrometer:micrometer-core")
    // Argon2PasswordEncoder (Security.md §4.5) needs a real Argon2 implementation at runtime;
    // Spring Security Crypto only declares the dependency as optional.
    runtimeOnly("org.bouncycastle:bcprov-jdk18on:1.79")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
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
