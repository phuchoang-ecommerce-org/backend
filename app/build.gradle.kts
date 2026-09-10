plugins {
    id("org.springframework.boot")
    java
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.modulith:spring-modulith-starter-core")

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
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.jmolecules.archunit)
    testImplementation(libs.jmolecules.ddd)
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
