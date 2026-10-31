import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
     
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.daniel.votacioneselecciones"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.daniel.votacioneselecciones"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.cardview:cardview:1.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.firebase:firebase-messaging")
}

 
 
val adbDebugView = androidComponents.sdkComponents.adb
val fuenteActivador = rootProject.layout.projectDirectory.file("tools/ActivarDebugView.java")
val paqueteDebugView = android.defaultConfig.applicationId.orEmpty()
val activarDebugView = tasks.register<Exec>("activarDebugView") {
    group = "verification"
    description = "Activa y verifica DebugView en los dispositivos Android conectados."
    inputs.file(fuenteActivador)
     
    isIgnoreExitValue = true
    doFirst {
        val nombreJava = if (System.getProperty("os.name").contains("Windows", true)) "java.exe" else "java"
        val ejecutableJava = File(System.getProperty("java.home"), "bin/" + nombreJava)
        commandLine(ejecutableJava.absolutePath, "--source", "17",
            fuenteActivador.asFile.absolutePath, adbDebugView.get().asFile.absolutePath, paqueteDebugView)
    }
}
tasks.matching { it.name == "assembleDebug" || it.name == "installDebug" }.configureEach {
    finalizedBy(activarDebugView)
}
