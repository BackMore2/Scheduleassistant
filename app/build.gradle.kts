plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.backmo.scheduleassistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.backmo.scheduleassistant"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.fragment)
    implementation(libs.mpandroidchart)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.room.runtime)
    implementation(libs.androidx.work.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 本地 AAR 条件引入（将 AAR 放入 app/libs 后自动生效）
    implementation(files("libs/Lite3dMap_1.3.2_AMapSearch_9.7.4_AMapLocation_6.5.1_20251020.aar"))
}
