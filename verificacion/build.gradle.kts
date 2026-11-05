plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies { implementation("com.google.ai.edge.litertlm:litertlm-jvm:0.18.1") }
application { mainClass.set("VerificarModeloKt"); applicationDefaultJvmArgs = listOf("-Xmx512m") }
tasks.named<JavaExec>("run") {
    dependsOn(rootProject.tasks.named("prepararModelos"))
    args(rootProject.file("modelos/src/main/assets/modelos/asistente-qwen3.litertlm").absolutePath)
}
