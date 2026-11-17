plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "co.liquida.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "co.liquida.app"
        minSdk = 24
        targetSdk = 35
        versionCode = runNumber
        versionName = "0.1.$runNumber"
    }

    // Llave de desarrollo: permite actualizar sin desinstalar. Para Play Store se usará otra llave privada.
    signingConfigs {
        create("dev") {
            storeFile = file("dev.keystore")
            storePassword = "liquida123"
            keyAlias = "dev"
            keyPassword = "liquida123"
        }
    }
    buildTypes {
        debug { signingConfig = signingConfigs.getByName("dev") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("dev")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
}
