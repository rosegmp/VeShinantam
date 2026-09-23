import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val releaseSigningPropertyNames = listOf(
    "releaseStoreFile",
    "releaseStorePassword",
    "releaseKeyAlias",
    "releaseKeyPassword",
)
val suppliedReleaseSigningProperties = releaseSigningPropertyNames.filter { providers.gradleProperty(it).isPresent }
require(suppliedReleaseSigningProperties.isEmpty() || suppliedReleaseSigningProperties.size == releaseSigningPropertyNames.size) {
    "Release signing is only partially configured. Set all of: ${releaseSigningPropertyNames.joinToString()}."
}
val releaseSigningConfigured = suppliedReleaseSigningProperties.isNotEmpty()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.androidx.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "app.veshinantam"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.veshinantam"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "0.1.11"
        val presetCatalogUpdateUrl = providers.gradleProperty("presetCatalogUpdateUrl").orElse("").get()
        val presetCatalogPublicKey = providers.gradleProperty("presetCatalogPublicKey").orElse(
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEx3gli60LF/hLHlkuh9AqlgM3rpo3bP5L3fQySwJH8ggsHmMMmgSdisKnL+ljpJGSuEqE82uN/UBItAYlPpfZpA==",
        ).get()
        buildConfigField("String", "PRESET_CATALOG_UPDATE_URL", "\"${presetCatalogUpdateUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "PRESET_CATALOG_PUBLIC_KEY", "\"${presetCatalogPublicKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"https://tyzembsyzzjrdhmmmfln.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_VpyDD3COjLuAKkNmaPaBgQ_sKGiIGXe\"")

        testInstrumentationRunner = "app.veshinantam.data.local.MigrationTestInstrumentation"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(providers.gradleProperty("releaseStoreFile").get())
                storePassword = providers.gradleProperty("releaseStorePassword").get()
                keyAlias = providers.gradleProperty("releaseKeyAlias").get()
                keyPassword = providers.gradleProperty("releaseKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation(libs.junit)
    testImplementation(files("libs/json-20240303.jar"))
}
