# VotacionesElecciones

Aplicación Android de elecciones, candidatos y votaciones con Firebase Auth/Firestore, Crashlytics y notificaciones.

Tres librerías Android externas: **Vosk** para voz a texto offline, **LiteRT-LM** para chatbot local y **PdfBox-Android** para PDF. La versión **1.4** incluye los modelos español de Vosk y Qwen3 0.6B: el usuario no descarga ni importa archivos. Solo espera la preparación automática inicial y concede micrófono para dictar.

Android Studio obtiene los pesos al compilar y el APK **debug** los incluye. Para Play, **bundleRelease** los entrega en un asset pack **install-time**; el AAB debe firmarse y probarse en Play Console. No distribuyas el APK release suelto sin su paquete de modelos.

Consulta [instalación, licencias, requisitos, tamaños y pruebas](docs/HERRAMIENTAS_GRATUITAS.md). Los modelos añaden unos 650 MB a la instalación y requieren RAM/espacio libres; son Apache-2.0. No se usa Firebase AI Logic ni una API remota de chatbot. El panel de evaluación está eliminado.

Compila con Java 17 y Android SDK 36: `./gradlew :app:assembleDebug :app:bundleRelease :app:testDebugUnitTest`. Mínimo API 24, arm64-v8a. CI ejecuta además inferencia real del chatbot en host y entrega APK/AAB en los artefactos `app-lista-para-instalar` y `bundle-play-con-modelos`.
