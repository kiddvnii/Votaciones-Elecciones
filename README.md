# VotacionesYElecciones

Aplicación Android para administrar elecciones, candidatos y votos. Usa Firebase para las cuentas y los datos, e incluye dictado en español, un asistente local y reportes PDF.

## Abrir el proyecto

Necesitas Android Studio, Java 17 y Android SDK 36. La app funciona desde Android 7.0 (API 24), en teléfonos arm64-v8a y emuladores x86_64.

1. Abre el repositorio en Android Studio y sincroniza Gradle.
2. Comprueba que `app/google-services.json` corresponda a tu proyecto Firebase.
3. Publica `firestore.rules` en **Firebase Console → Firestore Database → Reglas**.
4. Ejecuta la variante **debug**.

La primera compilación descarga y verifica los modelos. El APK debug los incluye; al abrir las herramientas, la app prepara una copia privada. Reserva varios GB libres y suficiente memoria para el asistente.

## Librerías

| Función | Librería | Código |
|---|---|---|
| Voz a texto | Vosk Android 0.3.75 | [TranscripcionActivity.kt](app/src/main/java/com/daniel/votacionesyelecciones/TranscripcionActivity.kt) |
| Asistente local | LiteRT-LM 0.18.1 | [AsistenteActivity.kt](app/src/main/java/com/daniel/votacionesyelecciones/AsistenteActivity.kt) |
| Reportes PDF | PdfBox-Android 2.0.27.0 | [ReportePdf.kt](app/src/main/java/com/daniel/votacionesyelecciones/ReportePdf.kt) |

Las dependencias están en [app/build.gradle.kts](app/build.gradle.kts). El dictado y el asistente procesan los datos en el dispositivo; Firebase requiere conexión para consultar y guardar registros.

## Compilar

```bash
./gradlew :app:assembleDebug :app:bundleRelease :app:testDebugUnitTest
```

Para Google Play, usa el AAB: entrega los modelos en un paquete install-time. Debes firmarlo y probarlo en Play Console. El APK release suelto no incluye ese paquete.

GitHub Actions genera los artefactos `app-lista-para-instalar` y `bundle-play-con-modelos`.

Consulta la [guía de herramientas y configuración](docs/HERRAMIENTAS_GRATUITAS.md) para modelos, licencias, calendario y solución de problemas.
