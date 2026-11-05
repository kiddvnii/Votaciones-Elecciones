# Tres librerías externas gratuitas para Android

Implementación del PR #1. Las tres funciones llaman a dependencias externas reales; no son accesos al dictado/PDF del SDK ni llamadas a Firebase AI Logic. Sin pagos por consulta. Los modelos se descargan aparte y tienen licencias propias.

| Función | Dependencia fijada | Licencia del código | Uso real |
|---|---|---|---|
| Voz a texto | `com.alphacephei:vosk-android:0.3.75@aar` | Apache-2.0 | `Model`, `Recognizer`, `SpeechService`, micrófono PCM 16 kHz |
| Chatbot local | `com.google.ai.edge.litertlm:litertlm-android:0.18.1` | Apache-2.0 | `Engine`, `Conversation.sendMessage`, backend CPU |
| Reporte PDF | `com.tom-roush:pdfbox-android:2.0.27.0` | Apache-2.0 | `PDDocument`, `PDPageContentStream`, guardado con selector de documentos |

Vosk usa además JNA `5.18.1@aar` (LGPL-2.1-or-later / Apache-2.0, elección dual). Se mantienen Firebase Auth, Firestore, Analytics, Crashlytics, Messaging, App Check y notificaciones. Se elimina `firebase-ai`. App Check sigue protegiendo los servicios Firebase existentes; registra el token debug en desarrollo y Play Integrity para publicación si exiges App Check en consola. No se requiere habilitar AI Logic, Gemini ni facturación para estas tres herramientas. El panel de pruebas sigue eliminado.

## Compatibilidad y decisión

Revisión de fuentes oficiales: 5 de noviembre de 2026. El enlace de MediaPipe LLM Inference ya redirige a LiteRT-LM y la clase `LlmInference` está deprecada. Por eso se usa su sucesor, también una librería Android externa, en lugar de agregar una integración obsoleta. LiteRT-LM 0.18.1 tiene metadata Kotlin 2.4; el proyecto usa Kotlin 2.4.21. Se fija la versión para evitar cambios imprevistos.

La distribución de esta app se limita a **arm64-v8a** para el motor LLM. No sirve para un teléfono de 32 bits ni para un emulador x86. Compilar un APK no demuestra que su motor nativo o un modelo funcionen en cualquier teléfono. Verifica especialmente dispositivos Android con páginas de memoria de 16 KB antes de publicar. PdfBox-Android declara API 19+; Vosk demo usa API 21+. El AAR descargado de LiteRT-LM 0.18.1 declara API 24, compatible con minSdk 24 de esta app. Se comprobó que los segmentos PT_LOAD de las tres bibliotecas nativas arm64 (LiteRT-LM, Vosk y JNA) usan alineación de 16384 bytes; CI comprueba además la alineación ZIP del APK. No se fuerzan incompatibilidades con `overrideLibrary`.

## Demostración de voz

1. Descarga el ZIP original **vosk-model-small-es-0.42** desde [Vosk Models](https://alphacephei.com/vosk/models). El catálogo indica 39 MB, licencia Apache-2.0. Un modelo pequeño suele requerir unos 300 MB de RAM; la cifra varía.
2. Menú → Voz a texto → Importar modelo Vosk ZIP. No lo descomprimas antes. El importador limita entradas y tamaño expandido a 512 MiB, impide rutas fuera de su carpeta y comprueba la carga nativa antes de instalarlo.
3. Concede micrófono, pulsa Hablar, dicta una frase en español y pulsa Detener. Edita el resultado y guarda un TXT en la ubicación elegida. No se guarda audio ni se envía a un servidor. La grabación se detiene al salir de la pantalla.
4. Cierra y abre de nuevo para comprobar la persistencia del modelo y prueba sin conexión. La precisión depende de ruido, acento y vocabulario; no se garantiza transcripción exacta.

## Demostración del chatbot

1. Consulta [Gemma3-1B-IT de litert-community](https://huggingface.co/litert-community/Gemma3-1B-IT/tree/main). Inicia sesión y acepta los términos de **Gemma** si el repositorio lo exige. Es una licencia propia del modelo, distinta de Apache-2.0 del motor. Revisa términos y restricciones antes de distribuir pesos.
2. Descarga **Gemma3-1B-IT_multi-prefill-seq_q4_ekv4096.litertlm** (el catálogo consultado indica 584 MB). Evita las variantes con sufijos de chips/NPU; se usa el backend CPU. No sirven GGUF de llama.cpp, `.task` de MediaPipe ni renombrar una extensión. Los pesos NO se incluyen en Git ni en el APK; tampoco se aceptan términos automáticamente.
3. Menú → Asistente local → Importar modelo .litertlm. La app copia hasta 4 GiB en almacenamiento privado y verifica la inicialización antes de reemplazar el archivo anterior. Reserva espacio para el original, la copia y, al reemplazar, ambos modelos. Modelos de alrededor de 1B cuantizados pueden ocupar cientos de MB y necesitan más RAM en ejecución; no hay una cifra universal ni garantía por versión Android. Para la demostración se recomienda un teléfono arm64 con 6–8 GB de RAM y varios GB libres, como recomendación práctica, no mínimo certificado.
4. Espera “Modelo local listo”, envía una pregunta, otra de seguimiento y usa Nueva conversación para borrar el contexto. Se limita el contexto a 1024 tokens y las consultas a 2000 caracteres. Si se agota el contexto, reinicia. La carga y generación se ejecutan fuera del hilo de interfaz, en orden; al cerrar se liberan conversación y motor después del trabajo en curso.
5. Sin modelo, Enviar permanece deshabilitado. Un archivo inválido o un dispositivo incompatible muestra un fallo; no se inventan respuestas de respaldo ni se presenta un saludo como inferencia. Una falta extrema de RAM o un fallo en código nativo puede terminar el proceso y no siempre es recuperable con una excepción. El historial visible dura hasta cerrar/recrear la pantalla; los pesos persisten. El chatbot no accede a Firestore, votos ni información electoral en tiempo real.

## Demostración del PDF

Menú → Reporte PDF o Votaciones → Exportar PDF. Elige destino. Se consultan elecciones, candidatos y votos de la sesión mediante el repositorio Firestore existente y se escribe con PdfBox fuera del hilo principal. Incluye totales y paginación de textos largos; no añade datos personales del votante. Una consulta Firestore puede requerir conexión. La generación PDF en sí es local y no requiere modelo. Helvetica conserva caracteres españoles compatibles con WinAnsi; otros símbolos (por ejemplo emoji) se reemplazan por `?` para evitar abortar el reporte. El archivo puede quedar parcial si falla la escritura: elimina ese archivo y reintenta.

## Validación

CI ejecuta `:app:assembleDebug :app:assembleRelease :app:testDebugUnitTest` en Java 17 y Android SDK 36. Incluye pruebas del importador: ZIP traversal, estructura Vosk, límite de copia y archivo vacío. No descarga modelos ni requiere credenciales de IA. Compilación y pruebas unitarias no equivalen a pruebas de micrófono, inferencia o visualización PDF en un teléfono.

Pruebas manuales pendientes en dispositivo: importación/carga y persistencia de ambos modelos; permisos denegados; ZIP/modelo inválidos; falta de espacio; dictado offline y detención al salir; conversación de seguimiento, reinicio/contexto lleno y navegación durante generación; PDF vacío, varias páginas, acentos, emoji, cancelación y errores de destino. Verifica además login, CRUD Firestore, notificaciones y reportes Crashlytics con la configuración del proyecto.

## Fuentes y licencias

- [Vosk Android demo y versiones](https://github.com/alphacep/vosk-android-demo/blob/master/app/build.gradle), [Vosk licencia](https://github.com/alphacep/vosk-api/blob/master/COPYING), [catálogo de modelos](https://alphacephei.com/vosk/models).
- [LiteRT-LM Android](https://developers.google.com/edge/litert-lm/android), [compatibilidad Kotlin](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md), [licencia](https://github.com/google-ai-edge/LiteRT-LM/blob/main/LICENSE), [MediaPipe deprecado](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/java/com/google/mediapipe/tasks/genai/llminference/LlmInference.java).
- [Gemma modelo y términos](https://huggingface.co/litert-community/Gemma3-1B-IT), [términos Gemma](https://ai.google.dev/gemma/terms).
- [PdfBox-Android: versión, inicialización, API y licencia](https://github.com/TomRoush/PdfBox-Android), [NOTICE](https://github.com/TomRoush/PdfBox-Android/blob/master/NOTICE.txt).
- [JNA licencias](https://github.com/java-native-access/jna/blob/master/LICENSE).

Antes de redistribuir el APK, conserva licencias y avisos de las dependencias y revisa sus componentes transitivos. Si distribuyes modelos, incorpora además sus términos y avisos. Esta integración no descarga ni redistribuye pesos.
