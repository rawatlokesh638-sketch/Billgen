import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.billgen.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  val keystoreFile: File = run {
    val customPath = System.getenv("KEYSTORE_PATH")
    if (!customPath.isNullOrEmpty() && file(customPath).exists()) {
      file(customPath)
    } else {
      val rootDebug = file("${rootDir}/debug.keystore")
      if (!rootDebug.exists()) {
        val b64File = file("${rootDir}/debug.keystore.base64")
        val b64Content = if (b64File.exists()) {
          b64File.readText().trim()
        } else {
          "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"
        }
        try {
          val bytes = Base64.getDecoder().decode(b64Content)
          rootDebug.writeBytes(bytes)
        } catch (_: Exception) {}
      }
      rootDebug
    }
  }

  signingConfigs {
    create("release") {
      storeFile = keystoreFile
      val isCustom = System.getenv("KEYSTORE_PATH") != null && file(System.getenv("KEYSTORE_PATH")).exists()
      if (isCustom) {
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("STORE_PASSWORD") ?: "android"
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
      } else {
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = true
    }
    create("debugConfig") {
      storeFile = keystoreFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
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
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.database)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
