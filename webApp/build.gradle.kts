import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "veshinantam"
        browser {
            commonWebpackConfig {
                outputFileName = "veshinantam.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    packageOfResClass = "app.veshinantam.web.generated.resources"
}

tasks.register<Sync>("stageSite") {
    dependsOn("wasmJsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    doLast {
        val site = rootProject.layout.projectDirectory.dir("dist").asFile
        val worker = site.resolve("service-worker.js")
        val builtIn = setOf(
            "index.html", "veshinantam.js", "entity-sync.js", "preset-updates.js", "supabase-config.js",
            "manifest.webmanifest", "icon.svg", "service-worker.js",
        )
        val generatedFiles = site.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(site).invariantSeparatorsPath }
            .filterNot { it in builtIn || it.endsWith(".map") || it.endsWith(".LICENSE.txt") }
            .sorted()
            .toList()
        val entries = generatedFiles.joinToString(",\n  ") { "'./$it'" }
        val marker = "/*__PRECACHE_FILES__*/"
        val source = worker.readText()
        check(marker in source) { "Service-worker precache marker is missing." }
        worker.writeText(source.replace(marker, entries))
        val stagedWorker = worker.readText()
        check(marker !in stagedWorker) { "Service-worker precache manifest was not generated." }
        val runtimeAssets = generatedFiles.filter { it.endsWith(".wasm") }
        check(runtimeAssets.isNotEmpty() && runtimeAssets.all(stagedWorker::contains)) {
            "Every Wasm runtime must be present in the offline precache manifest."
        }
    }
}
