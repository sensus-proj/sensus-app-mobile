plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.kover)
  kotlin("kapt")
}

android {
    namespace = "com.example.sensus"
    compileSdk = 36
    defaultConfig {
        applicationId = "sensus.com"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

kover {
  reports {
    filters {
      includes {
        classes(
          "com.example.sensus.domain.*",
          "com.example.sensus.presentation.*"
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
          "*Utils*",
          "*Icons*",
          "*NavTab*",
          "com.example.sensus.presentation.common.*"
        )
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
