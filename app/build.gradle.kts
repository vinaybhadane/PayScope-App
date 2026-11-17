import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val apiBaseUrl = providers.gradleProperty("PAYSCOPE_API_BASE_URL").orElse("")
val firebaseLocal = Properties().apply {
    val configFile = rootProject.file("firebase.local.properties")
    if (configFile.isFile) configFile.reader(Charsets.UTF_8).use { load(it) }
}
fun firebaseValue(key: String): String = providers.gradleProperty(key)
    .orElse(providers.environmentVariable(key)).orElse(firebaseLocal.getProperty(key, "")).get()
fun javaString(value: String): String = "\"" + value.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
val firebaseApiKey = firebaseValue("PAYSCOPE_FIREBASE_API_KEY")
val firebaseAppId = firebaseValue("PAYSCOPE_FIREBASE_APP_ID")
val firebaseProjectId = firebaseValue("PAYSCOPE_FIREBASE_PROJECT_ID")
val googleWebClientId = firebaseValue("PAYSCOPE_GOOGLE_WEB_CLIENT_ID")
val firebaseConfigured = listOf(firebaseApiKey, firebaseAppId, firebaseProjectId, googleWebClientId).all { it.isNotBlank() }

tasks.register("verifyFirebaseConfiguration") {
    doLast {
        check(firebaseConfigured) { "Configure firebase.local.properties before building a sign-in-enabled APK. See README.md." }
    }
}

android {
    namespace = "com.payscope.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.payscope.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
        buildConfigField("String", "FIREBASE_API_KEY", javaString(firebaseApiKey))
        buildConfigField("String", "FIREBASE_APP_ID", javaString(firebaseAppId))
        buildConfigField("String", "FIREBASE_PROJECT_ID", javaString(firebaseProjectId))
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", javaString(firebaseValue("PAYSCOPE_FIREBASE_STORAGE_BUCKET")))
        buildConfigField("String", "FIREBASE_SENDER_ID", javaString(firebaseValue("PAYSCOPE_FIREBASE_SENDER_ID")))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", javaString(googleWebClientId))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Firebase & Auth
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
