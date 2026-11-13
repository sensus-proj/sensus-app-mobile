import org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask
import java.io.File
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    kotlin("kapt")
}

android {
    namespace = "com.example.sensus"
    compileSdk = 36
    val ciVersionCode =
        providers.gradleProperty("SENSUS_VERSION_CODE").orNull?.toIntOrNull()
            ?: providers.environmentVariable("SENSUS_VERSION_CODE").orNull?.toIntOrNull()
    val baseVersionCode = 8

    defaultConfig {
        applicationId = "sensus.com"
        minSdk = 24
        targetSdk = 36
        versionCode = maxOf(baseVersionCode, ciVersionCode ?: baseVersionCode)
        versionName = "1.3.0"
    }

    val keysPropertiesText =
        providers.fileContents(
            rootProject.layout.projectDirectory.file("keys.properties"),
        ).asText.orNull
    val keysProperties =
        Properties().apply {
            if (!keysPropertiesText.isNullOrBlank()) {
                load(StringReader(keysPropertiesText))
            }
        }

    fun signingSetting(key: String): String? =
        providers.environmentVariable(key).orNull?.trim()?.takeIf { it.isNotEmpty() }
            ?: keysProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

    val releaseStorePath = signingSetting("SIGNING_STORE_FILE")
    val releaseStorePassword = signingSetting("SIGNING_STORE_PASSWORD")
    val releaseKeyAlias = signingSetting("SIGNING_KEY_ALIAS")
    val releaseKeyPassword = signingSetting("SIGNING_KEY_PASSWORD")
    val releaseStoreFile =
        releaseStorePath?.let { path ->
            val candidate = File(path)
            if (candidate.isAbsolute) candidate else rootProject.layout.projectDirectory.file(path).asFile
        }?.takeIf { it.isFile }
    if (releaseStorePath != null && releaseStoreFile == null) {
        logger.warn("WARNING: SIGNING_STORE_FILE is set, but keystore file was not found.")
    }
    val hasReleaseSigning =
        releaseStoreFile != null &&
            releaseStorePassword != null &&
            releaseKeyAlias != null &&
            releaseKeyPassword != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Sprint 1 Core Libraries: Coil, Location, Crypto & Serialization
    implementation(libs.coil.compose)
    implementation(libs.play.services.location)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.osmdroid.android)

    // Networking (Retrofit 2 & OkHttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Unit & Flow Testing
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Local Encrypted Persistence (Room + SQLCipher AES-256)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.framework)
}

// kapt { correctErrorTypes = false }

val sqliteNativeTmpPath = File(gradle.gradleUserHomeDir, "tmp/sqlite").apply { mkdirs() }.absolutePath
if (System.getProperty("org.sqlite.tmpdir").isNullOrBlank()) {
    System.setProperty("org.sqlite.tmpdir", sqliteNativeTmpPath)
}

tasks.withType<KaptWithoutKotlincTask>().configureEach {
    kaptProcessJvmArgs.addAll(
        listOf(
            "-Dorg.sqlite.tmpdir=$sqliteNativeTmpPath",
            "-Djava.io.tmpdir=$sqliteNativeTmpPath",
        ),
    )
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "com.example.sensus.domain.*",
                    "com.example.sensus.presentation.*",
                )
            }
            excludes {
                classes(
                    "*_Factory*",
                    "*_HiltModules*",
                    "*Hilt_*",
                    "*_MembersInjector*",
                    "*_Impl*",
                    "*BuildConfig*",
                    "*ComposableSingletons*",
                    "*Screen*Kt*",
                    "*Screens*Kt*",
                    "*Modal*Kt*",
                    "*Dialog*Kt*",
                    "*Dialogs*Kt*",
                    "*Sheet*Kt*",
                    "*Container*Kt*",
                    "*Components*Kt*",
                    "*Overlays*Kt*",
                    "*Menu*Kt*",
                    "*Utils*",
                    "*Icons*",
                    "*NavTab*",
                    "com.example.sensus.presentation.common.*",
                )
                annotatedBy("com.example.sensus.presentation.common.ComposeUiModel")
            }
        }
        variant("debug") {
            verify {
                rule {
                    minBound(80)
                }
            }
        }
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
    parallel = true
    ignoreFailures = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}
