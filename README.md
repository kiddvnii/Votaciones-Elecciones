# VotacionesElecciones

Aplicación Android de elecciones, candidatos y votaciones con Firebase Auth/Firestore, Crashlytics y notificaciones.

El PR #1 integra tres librerías Android externas gratuitas: **Vosk** (voz a texto offline), **LiteRT-LM** (chatbot IA local) y **PdfBox-Android** (reportes). Las funciones de IA requieren importar modelos compatibles; no vienen en el APK. Sin modelo no se habilita la función correspondiente.

Consulta [instalación, licencias, requisitos y pruebas](docs/HERRAMIENTAS_GRATUITAS.md). No se usa Firebase AI Logic ni una API de chatbot de pago. El panel de pruebas de evaluación está eliminado.

Compilación: Java 17, Android SDK 36, `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest`. Revisa el mínimo Android declarado en `app/build.gradle.kts`; APK para arm64-v8a. La comprobación de funcionamiento con modelos debe hacerse en dispositivo real.
