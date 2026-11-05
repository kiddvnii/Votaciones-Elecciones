# Herramientas y configuración

## Voz, asistente y PDF

| Función | Dependencia | Uso |
|---|---|---|
| Dictado en español | `com.alphacephei:vosk-android:0.3.75@aar` | Reconocimiento a 16 kHz con Model, Recognizer y SpeechService |
| Asistente local | `com.google.ai.edge.litertlm:litertlm-android:0.18.1` | Engine y Conversation con CPU |
| Exportación PDF | `com.tom-roush:pdfbox-android:2.0.27.0` | PDDocument y PDPageContentStream |

Vosk también usa JNA 5.18.1. Las tres librerías son externas al SDK Android. La app conserva Firebase Auth, Firestore, Analytics, Crashlytics, Messaging y App Check.

El dictado permite editar el texto y guardarlo como TXT. El borrador se conserva por usuario; pulsar **Borrar** lo elimina. El archivo solo se crea al elegir **Guardar TXT** y un destino.

El asistente usa Qwen3 0.6B. Tiene un contexto de 2048 tokens y respuestas de hasta 256 tokens, con topK=1 y temperatura 0. Las sugerencias rellenan la pregunta. El modelo puede equivocarse y no consulta datos electorales ni internet.

## Modelos incluidos

La app prepara los modelos desde los archivos de instalación, sin descargarlos durante el uso. La primera apertura copia o descomprime los recursos y muestra el progreso. Las siguientes reutilizan la copia privada.

- **Voz:** Vosk español pequeño 0.42, unos 39 MB comprimidos.
- **Asistente:** Qwen3-0.6B INT8 para LiteRT-LM, unos 614 MB.

Reserva varios GB libres para la instalación, la extracción y las copias privadas. El asistente también necesita memoria disponible; su ejecución depende del dispositivo.

Los modelos se descargan al compilar mediante `prepararModelos`, en `modelos/src/main/assets/modelos`. Esa carpeta está ignorada por Git. La tarea comprueba tamaño y SHA-256 y detiene la compilación si los archivos no coinciden.

| Archivo | SHA-256 |
|---|---|
| vosk-model-small-es-0.42.zip | `09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f` |
| Qwen3-0.6B.litertlm | `c5f39b4191926786c3e6668b62af4d6eb15248dfc7a5348252a9b406f33f2108` |

Qwen está fijado a la revisión `e6687177e00897731e7716267eebd4eba0dde7c2` de litert-community. Ambos modelos se distribuyen sin modificar sus pesos, bajo Apache-2.0. La app incluye la licencia, las atribuciones y la ficha del modelo; se consultan desde **Licencias**.

## Compilación y distribución

Usa Java 17, Android SDK 36 y la variante debug para probar desde Android Studio. El APK debug contiene ambos modelos.

Para Google Play, genera `bundleRelease`: el módulo `modelos` es un asset pack install-time. El AAB requiere firma de subida y pruebas en Play Console. El APK release independiente no contiene el paquete de modelos.

```bash
./gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest
./gradlew :verificacion:run
python3 tools/verificar_paquetes.py
```

La prueba JVM de inferencia requiere Java 21. GitHub Actions compila las variantes, ejecuta pruebas unitarias y de reglas, verifica modelos, licencias y alineación de paquetes, y prueba LiteRT-LM y Vosk en Android API 36 x86_64. La prueba de Vosk procesa silencio; hay que probar el dictado con un micrófono real. Las respuestas del asistente se comprueban para detectar fallos de ejecución, sin certificar su exactitud.

## Elecciones y Firebase

El calendario permite elegir hoy o fechas futuras. El término se propone a tres meses calendario desde la creación y se puede modificar. Si el mes de destino es más corto, se usa su último día. El término no puede ser anterior al inicio.

Al editar una elección histórica se pueden conservar fechas pasadas sin cambiarlas. Los documentos antiguos sin término se siguen leyendo; al editarlos se propone un término para revisar y guardar. El estado Activa/Cerrada se cambia por separado.

Publica `firestore.rules` en **Firebase Console → Firestore Database → Reglas** antes de guardar con el campo `fechaTermino`. Actualizar el repositorio no publica las reglas. Los registros se separan por `ownerUid`; las reglas verifican campos, relaciones y fechas. Para fechas sin hora admiten un día UTC de margen, mientras la app valida el día local.

## Problemas frecuentes

**El emulador no reconoce palabras:** en sus controles **⋮ → Microphone**, activa **Virtual microphone uses host audio input**. Revisa el permiso de micrófono de Windows para aplicaciones de escritorio. El indicador verde de Android confirma que se abrió el micrófono virtual, pero no que reciba audio del equipo.

**El asistente se cierra al abrir:** instala el APK actualizado. Se incluyen bibliotecas nativas arm64-v8a y x86_64; una instalación ARM traducida en un emulador x86 puede fallar.

**No se guarda un registro:** revisa el código que muestra el aviso. En Logcat, filtra por `OperacionFirebase` para ver la excepción completa. Comprueba la sesión, las reglas publicadas y los propietarios de los documentos relacionados.

## Referencias y licencias

- [Vosk Android](https://github.com/alphacep/vosk-android-demo), [modelos y licencias](https://alphacephei.com/vosk/models).
- [LiteRT-LM Android](https://developers.google.com/edge/litert-lm/android), [licencia](https://github.com/google-ai-edge/LiteRT-LM/blob/main/LICENSE).
- [Conversión Qwen fijada](https://huggingface.co/litert-community/Qwen3-0.6B/tree/e6687177e00897731e7716267eebd4eba0dde7c2), [modelo y licencia originales](https://huggingface.co/Qwen/Qwen3-0.6B).
- [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android), [NOTICE](https://github.com/TomRoush/PdfBox-Android/blob/master/NOTICE.txt).
- [JNA](https://github.com/java-native-access/jna/blob/master/LICENSE), [Play Asset Delivery](https://developer.android.com/guide/playcore/asset-delivery/integrate-java), [micrófono del emulador](https://developer.android.com/studio/run/emulator-extended-controls).
