plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.hood.remote"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.hood.remote"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    val ksPath = System.getenv("KEYSTORE_PATH")
    val ksPass = System.getenv("KEYSTORE_PASSWORD")
    val ksAlias = System.getenv("KEY_ALIAS")
    val ksKeyPass = System.getenv("KEY_PASSWORD")
    val hasSigning = !ksPath.isNullOrBlank() && !ksPass.isNullOrBlank() &&
        !ksAlias.isNullOrBlank() && !ksKeyPass.isNullOrBlank()

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(ksPath!!)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksKeyPass
            }
        }
    }
    buildTypes {
        release {
            // Your signature if all 4 secrets exist, otherwise the debug key
            signingConfig = if (hasSigning) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
    }
    lint { abortOnError = false }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
