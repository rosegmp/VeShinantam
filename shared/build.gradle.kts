import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

val fullBrowserMatrix = providers.gradleProperty("fullBrowserMatrix")
    .map(String::toBoolean)
    .orElse(false)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    if (fullBrowserMatrix.get()) {
                        useFirefoxHeadless()
                    }
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
