rootProject.name = "ecp"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "shared-kernel",
    "identity",
    "catalog",
    "inventory",
    "cart",
    "ordering",
    "payment",
    "shipping",
    "promotion",
    "review",
    "notification",
    "audit",
    "reporting",
    "app"
)

// Keep the Gradle project paths stable while grouping all bounded-context
// libraries beneath one physical module root.  This preserves the dependency
// graph from Module Dependency Diagram.md (§2–§3): consumers continue to
// declare dependencies such as project(":identity"), rather than acquiring a
// new nested project path.
listOf(
    "shared-kernel",
    "identity",
    "catalog",
    "inventory",
    "cart",
    "ordering",
    "payment",
    "shipping",
    "promotion",
    "review",
    "notification",
    "audit",
    "reporting"
).forEach { module ->
    project(":$module").projectDir = file("modules/$module")
}
