plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "jp.gr.java_conf.syntarou.midione"

    defaultConfig {
        applicationId = "jp.gr.java_conf.syntarou.midione"
        minSdk = 28
        targetSdk = 35
        compileSdk = 35
        versionCode = 1
        versionName = "1.0"

        versionNameSuffix = "1"

        androidResources.localeFilters += listOf("en", "ja")

        testApplicationId = buildToolsVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks {
        withType<JavaCompile> {
            options.compilerArgs.add("-Xlint:unchecked")
            options.compilerArgs.add("-deprecation")
        }
    }

    viewBinding {
        enable = true
    }

    dataBinding {
        enable = true
    }
    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "35.0.1"
    kotlinOptions {
        jvmTarget = "1.8"
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.legacy.support.v4)
    implementation(libs.core.ktx)
    implementation(libs.recyclerview)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    //implementation(libs.oss.licenses.plugin)
    implementation(libs.play.services.oss.licenses)
}
