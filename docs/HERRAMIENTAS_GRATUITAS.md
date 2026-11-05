# Voz, chatbot y PDF: librerías externas con modelos incluidos

## Experiencia al instalar

La app 1.4.1 incluye **Vosk español pequeño 0.42** y **Qwen3 0.6B para LiteRT-LM**. Ya no pide descargar ni importar modelos. Al abrir cada herramienta por primera vez, copia o descomprime sus recursos en almacenamiento privado y muestra el progreso. No hace ninguna solicitud de red para preparar ni usar los modelos. En aperturas posteriores reutiliza esa copia.

El primer inicio necesita espacio libre adicional: el modelo LLM ocupa aproximadamente 614 MB (586 MiB), el ZIP de voz unos 39 MB y la extracción de voz requiere más espacio. Cuenta además la instalación y las copias privadas; reserva varios GB libres. La preparación puede tardar y no equivale a una descarga. Se necesita un dispositivo arm64 con suficiente RAM; la inclusión de pesos no garantiza que todos los teléfonos puedan ejecutar el motor nativo. Un cierre durante la preparación permite reintentar en la siguiente apertura sin instalar archivos parciales como válidos.

Las pantallas usan tarjetas, botones Material, burbujas de conversación, sugerencias, estados de preparación/error y márgenes reales para barras del sistema, cámara y teclado. El dictado tiene vista parcial, texto editable, contador, borrado con confirmación y guardado TXT. El chat tiene conversación local, reinicio y respuestas limitadas a 256 tokens, con contexto de 2048 tokens. La generación usa selección greedy (topK=1, temperatura 0) para reducir respuestas aleatorias; eso no garantiza exactitud. No hay saludos ni respuestas de respaldo presentados como inferencia. Las sugerencias solo rellenan la pregunta.

## Tres librerías Android externas

| Función | Dependencia | Uso |
|---|---|---|
| Voz a texto offline | `com.alphacephei:vosk-android:0.3.75@aar` | Model, Recognizer, SpeechService con micrófono a 16 kHz |
| Chatbot IA local | `com.google.ai.edge.litertlm:litertlm-android:0.18.1` | Engine, Conversation, backend CPU |
| Reporte PDF | `com.tom-roush:pdfbox-android:2.0.27.0` | PDDocument y PDPageContentStream |

Se conserva JNA 5.18.1@aar como apoyo de Vosk. Firebase Auth, Firestore, Analytics, Crashlytics, Messaging, App Check y notificaciones siguen presentes. No se usa firebase-ai ni una API remota de IA. El panel de pruebas de evaluación permanece eliminado.

## Modelos y redistribución

- **Voz:** `vosk-model-small-es-0.42.zip`, obtenido de [Alpha Cephei](https://alphacephei.com/vosk/models), licencia Apache-2.0. SHA-256 `09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f`. Se entrega el ZIP original completo, con sus avisos y README. No se modifican los pesos.
- **Chat:** `Qwen3-0.6B.litertlm` de [litert-community/Qwen3-0.6B](https://huggingface.co/litert-community/Qwen3-0.6B), revisión `e6687177e00897731e7716267eebd4eba0dde7c2`. Licencia Apache-2.0 del [modelo original de Qwen](https://huggingface.co/Qwen/Qwen3-0.6B). SHA-256 `c5f39b4191926786c3e6668b62af4d6eb15248dfc7a5348252a9b406f33f2108`. Esta variante INT8 está documentada por sus autores para LiteRT-LM 0.18.0 o posterior. No se usan las variantes NPU, web ni los nuevos archivos con pesos externos que requieren otro runtime.
- Se reemplazó Gemma como modelo predeterminado para poder distribuir pesos Apache-2.0 sin pedir al usuario aceptar términos de Gemma o iniciar sesión en Hugging Face. El archivo de Qwen se empaqueta sin modificar. La configuración de conversación desactiva thinking y solicita respuestas breves en español.
- La licencia Apache completa, ficha original de conversión y atribuciones se distribuyen en recursos. En el asistente se accede a los avisos pulsando “Licencias”.

## Compilar e instalar

Requisitos: Java 17 recomendado para Android (Java 21 para la prueba JVM de inferencia), Android SDK 36, minSdk 24, destinos Java/Kotlin 17. Distribución arm64-v8a y x86_64 nativos. El AAR de LiteRT-LM declara API 24. Los segmentos PT_LOAD arm64 de LiteRT-LM, Vosk y JNA se comprobaron con alineación de 16384 bytes.

**Android Studio / APK para probar:** ejecuta normalmente la variante **debug**. La tarea raíz `prepararModelos` obtiene los pesos al compilar (solo la primera vez si la caché sigue válida); se necesita internet en el equipo de desarrollo. Los modelos se guardan en `modelos/src/main/assets/modelos`, ignorados por Git para no subir binarios gigantes. El APK debug incluye ambos modelos en el mismo archivo y no necesita pasos manuales en el teléfono.

**Google Play:** genera **bundleRelease**. El módulo `modelos` es un asset pack **install-time**: ambos modelos se entregan con la instalación antes de abrir la app. No se meten 650 MB en el módulo base de Play. El AAB debe firmarse con la clave de subida del propietario y validarse en pruebas internas de Play Console. El APK release suelto no contiene el asset pack: no lo distribuyas para probar esta experiencia; usa el APK debug completo o instala el AAB con todos sus paquetes. No se asegura aprobación de Play Console sin esa comprobación.

Comandos:

```text
./gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest
./gradlew :verificacion:run
python3 tools/verificar_paquetes.py
```

Si una descarga del equipo de compilación falla o el SHA-256 del chatbot no coincide, la compilación se detiene. No existe una opción silenciosa para omitir los modelos. CI entrega el APK debug completo y el AAB release (sin firma de subida) en los artefactos `app-lista-para-instalar` y `bundle-play-con-modelos`.

## Verificación

CI compila debug, release y bundleRelease; ejecuta pruebas unitarias del importador ZIP y copia limitada; comprueba los archivos reales dentro de APK/AAB y SHA-256 del LLM; verifica alineación ZIP a 16 KB. El módulo JVM `verificacion` usa LiteRT-LM **0.18.1**, los mismos pesos y CPU para ejecutar generación real y una pregunta de seguimiento, comprobando además una respuesta aritmética básica. Esa prueba de host no certifica la JNI Android ni sustituye probar un teléfono.

Pruebas de dispositivo necesarias: arranque sin red con instalación nueva, preparación interrumpida, dictado en español, permiso denegado, editar/borrar/guardar TXT, conversación y reinicio, límite de contexto, espacio/RAM insuficientes, rotación, teclado, texto grande, barra de estado/cámara, PDF vacío/varias páginas/acentos y regresiones de Auth/Firestore/notificaciones. La app requiere inicio de sesión; las consultas Firestore pueden necesitar internet, aunque estas dos herramientas procesan localmente.

## Fuentes

- [Vosk Android demo y versiones](https://github.com/alphacep/vosk-android-demo/blob/master/app/build.gradle), [Vosk licencia](https://github.com/alphacep/vosk-api/blob/master/COPYING), [modelos](https://alphacephei.com/vosk/models).
- [LiteRT-LM Android](https://developers.google.com/edge/litert-lm/android), [compatibilidad Kotlin](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md), [licencia](https://github.com/google-ai-edge/LiteRT-LM/blob/main/LICENSE).
- [Modelo Qwen fijado](https://huggingface.co/litert-community/Qwen3-0.6B/blob/e6687177e00897731e7716267eebd4eba0dde7c2/Qwen3-0.6B.litertlm), [licencia original Qwen](https://huggingface.co/Qwen/Qwen3-0.6B/blob/main/LICENSE).
- [Asset packs install-time y acceso AssetManager](https://developer.android.com/guide/playcore/asset-delivery/integrate-java).
- [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android), [NOTICE](https://github.com/TomRoush/PdfBox-Android/blob/master/NOTICE.txt), [JNA licencias](https://github.com/java-native-access/jna/blob/master/LICENSE).

## Corrección de cierre en emuladores x86_64 (1.4.1)

El registro del emulador del usuario mostraba ABI x86_64, guest architecture arm64, libnative_bridge_vdso y SIGSEGV dentro de liblitertlm_jni.so/nativeCreateEngine. La distribución anterior solo incluía ARM64 y forzaba traducción ARM en ese emulador. Ahora el APK/AAB incluye las bibliotecas nativas **x86_64 y arm64-v8a** de LiteRT-LM, Vosk y JNA. Android selecciona la biblioteca compatible al instalar; no se fuerza ARM en x86. Una comprobación adicional evita inicializar el asistente si detecta una instalación ARM traducida en x86.

CI añade una prueba **instrumentada Android**, independiente de la prueba de host: instala el APK completo y el APK de pruebas en un emulador API 36 x86_64, comprueba que la biblioteca seleccionada sea x86_64, copia los pesos incluidos, ejecuta nativeCreateEngine, genera texto y recibe una respuesta de seguimiento. Esta prueba detecta cierres nativos y respuestas vacías; no certifica exactitud factual. El modelo pequeño puede fallar incluso en preguntas sencillas. Un cierre nativo hace fallar la prueba aunque am instrument devuelva cero. Se guardan diagnóstico y Logcat. El resultado del cambio se documenta en su PR; esta prueba no certifica todos los teléfonos ni todas las versiones de Android.

Para actualizar desde la copia Git de Android Studio: `git pull --ff-only origin feature/alternativas-gratis-firebase`, sincronizar y ejecutar **app** otra vez. Debe instalarse el APK nuevo, no basta con volver a abrir la instalación anterior.

## Dictado sin palabras en emulador

El micrófono del PC no está habilitado por defecto en Android Emulator. En sus controles extendidos, abrir **Microphone** y activar **Virtual microphone uses host audio input**; revisar también el permiso de micrófono de Windows para aplicaciones de escritorio. El indicador verde de Android solo confirma que se abrió el micrófono virtual, no que esté llegando audio del PC. Ver [controles del emulador](https://developer.android.com/studio/run/emulator-extended-controls).

La vista parcial del dictado se muestra junto a los controles inferiores para que no quede oculta bajo el editor. Guardar TXT permite detener y recoger lo reconocido aunque aún no haya un segmento final. Si pasan diez segundos sin palabras, se muestra orientación para el micrófono. El borrador confirmado/editado persiste localmente por usuario; Borrar lo elimina. Esto no crea un archivo TXT hasta que se elige Guardar TXT y un destino.

## Fechas de elecciones

El formulario usa un calendario Material para inicio y término, sin escritura manual. Inicio por defecto hoy; término por defecto **tres meses de calendario desde la creación**, no 90 días ni desde el inicio seleccionado. Por ejemplo, 5 de noviembre → 5 de febrero; al caer en un mes más corto se usa su último día. El término es editable en el formulario y después al editar. No puede preceder al inicio. Si eliges un inicio posterior al término propuesto, debes ajustar el término antes de guardar.

Toda fecha nueva elegida debe ser hoy o futura según el día local del dispositivo. Al editar datos históricos se pueden conservar fechas pasadas sin cambiarlas; el calendario no permite seleccionar otra fecha pasada. Los documentos antiguos sin fechaTermino se siguen leyendo; al editarlos se propone hoy + 3 meses para que el usuario revise y guarde un término. No se migran datos automáticamente. Las fechas se muestran también en el listado y PDF. Esta modificación no crea un trabajo programado que cambie el estado Activa/Cerrada.

**Publicar las reglas nuevas antes de guardar con esta versión:** Firebase Console → Firestore Database → Reglas → reemplazar todo por `firestore.rules` del repositorio y pulsar Publicar. No basta con hacer git pull: las reglas de la consola son independientes. Se conserva la separación por ownerUid. Las reglas verifican formato/calendario y orden del período; toleran un día UTC en la restricción de fecha actual para cubrir zonas horarias, mientras calendario y repositorio aplican estrictamente el día local. CI valida las reglas en un emulador de Firestore con pruebas de propietario, acceso ajeno, fechas, campos inválidos, update y registros históricos. Son reglas prototipo para esta app; revisar y verificar antes de distribuir ampliamente.

La prueba instrumentada también carga el modelo español con Vosk/JNA x86_64 y procesa silencio verificando que no invente texto. No sustituye una prueba de voz con el micrófono del PC habilitado o en teléfono real.
