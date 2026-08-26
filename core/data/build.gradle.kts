plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.maurimax.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        // The portal every customer connects to. Customers never type a host —
        // they enter a username and password only. Set maurimax.portalUrl in
        // gradle.properties to point a build at your server.
        buildConfigField(
            "String",
            "PORTAL_URL",
            "\"${providers.gradleProperty("maurimax.portalUrl").getOrElse("http://portal.example.com:8080")}\"",
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core:model"))
    api(project(":core:network"))
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
