plugins {
    `java-library`
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":identity"))
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation(libs.jmolecules.ddd)
    implementation(libs.jmolecules.events)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    // Command aggregates use JPA; explicit query views use JdbcClient.  Spring Data JDBC is
    // available for simple relational projection persistence, not as a replacement for SQL
    // views that need joins, keyset pagination, or guarded upserts.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-json")

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
