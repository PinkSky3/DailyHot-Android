plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

val releaseKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
  ?: "${rootDir}/my-upload-key.jks"
val releaseStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
val hasReleaseSigning = file(releaseKeystorePath).isFile &&
  !releaseStorePassword.isNullOrBlank() &&
  !releaseKeyPassword.isNullOrBlank()

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.dailyhot.android"
    minSdk = 24
    targetSdk = 36
    versionCode = 6
    versionName = "1.3.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = file(releaseKeystorePath)
        storePassword = releaseStorePassword
        keyAlias = providers.environmentVariable("KEY_ALIAS").orNull ?: "upload"
        keyPassword = releaseKeyPassword
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    buildConfig = true
    compose = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  "ksp"(libs.moshi.kotlin.codegen)
}
