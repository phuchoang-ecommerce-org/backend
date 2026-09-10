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
