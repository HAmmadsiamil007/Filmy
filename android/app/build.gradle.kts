plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmy.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.filmy.app"
        minSdk = 24          # ExoPlayer/Media3 floor; covers ~97% of devices
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Overridable at build time:
        //   ./gradlew assembleDebug -PdefaultApiBase=http://192.168.1.10:8000
        // The in-app Settings screen still wins once the user sets it.
        buildConfigField(
            "String",
            "DEFAULT_API_BASE",
            "\"${project.findProperty("defaultApiBase") ?: ""}\""
        )
    }

    buildTypes {
        debug {
            // Local dev servers are plain http:// — allow cleartext so a
            // self-hosted API on the LAN works out of the box.
            isMinifyEnabled = false
        }
        release {
            // Shrink + obfuscate. Media3/kotlinx-serialization keep their
            // serializers via @Serializable, but OkHttp/Retrofit reflection
            // rules live in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Compose — BOM pins the whole UI stack to mutually compatible versions.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX core + lifecycle (viewModel + collectAsStateWithLifecycle).
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation: Home -> Detail -> Player, plus Settings.
    implementation(libs.androidx.navigation.compose)

    // Media3 / ExoPlayer. media3-dash is required: upstream serves .mpd DASH
    // manifests as well as progressive MP4, and without this ExoPlayer throws
    // on DASH content. See the player notes in docs/Roadmap/Android.md.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.dash)
    implementation(libs.media3.session)

    // HTTP + JSON. The backend returns snake_case JSON, so the serializer is
    // configured with namingStrategy = JsonNamingStrategy.SnakeCase in Api.kt.
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Poster loading (async + caching).
    implementation(libs.coil.compose)

    // Pure-logic unit tests (stream selection, URL building, classifiers).
    testImplementation(libs.junit)
}
