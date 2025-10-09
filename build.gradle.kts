plugins {
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.23" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.20" apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy {
            force(
                "org.jetbrains.kotlin:kotlin-stdlib:1.9.23",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.23",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.23",
                "org.jetbrains.kotlin:kotlin-stdlib-common:1.9.23",
                "org.jetbrains.kotlin:kotlin-reflect:1.9.23"
            )

            eachDependency {
                when (requested.group) {
                    "org.jetbrains.kotlin" -> useVersion("1.9.23")
                    "com.google.devtools.ksp" -> useVersion("1.9.23-1.0.20")
                }
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.android") {
        dependencies {
            add("implementation", platform("org.jetbrains.kotlin:kotlin-bom:1.9.23"))
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            add("implementation", platform("org.jetbrains.kotlin:kotlin-bom:1.9.23"))
        }
    }
}
