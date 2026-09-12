plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "org.phuchoang"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)
        "testCompileOnly"(rootProject.libs.lombok)
        "testAnnotationProcessor"(rootProject.libs.lombok)
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            // Sprint 03: identity needs Spring Boot-managed versions for its own starters
            // (data-jpa, security, data-redis) without becoming a Boot *application* module —
            // only :app applies the org.springframework.boot plugin. Every module now gets the
            // same managed versions :app already had implicitly, so a starter added to any
            // module never needs its own explicit version.
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.springBoot.get()}")
            mavenBom(rootProject.libs.spring.modulith.bom.get().toString())
            mavenBom(rootProject.libs.jmolecules.bom.get().toString())
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
