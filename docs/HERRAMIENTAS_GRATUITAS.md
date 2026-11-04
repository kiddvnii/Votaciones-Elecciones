# Herramientas gratuitas

La aplicación conserva Firebase Authentication, Cloud Firestore, Analytics, Crashlytics y Cloud Messaging. Se agregaron tres herramientas accesibles desde el menú principal:

- **Transcripción de voz:** usa el reconocimiento de voz disponible en Android y permite revisar y guardar un TXT local a través del selector de archivos. No utiliza la extensión de pago de Cloud Speech-to-Text ni sube el audio automáticamente a Cloud Storage. El reconocimiento depende de los servicios instalados en el teléfono y sus condiciones de uso.
- **Asistente con IA:** usa Firebase AI Logic + Gemini Developer API (modelo `gemini-3.8-flash`) con historial de chat durante la pantalla. No es la API de OpenAI ni la extensión de terceros para ChatGPT. Tiene límites de cuota gratuita. No se envían registros de votos al modelo.
- **Reporte PDF:** en Votaciones toca el botón de exportación de la barra superior, o entra desde el menú. Consulta las elecciones, candidatos y votos del usuario autenticado desde Firestore y crea un PDF local con `android.graphics.pdf.PdfDocument`. No necesita extensiones, almacenamiento cloud ni servicios de pago.

## Activar el asistente en Firebase

1. Abre [Firebase Console](https://console.firebase.google.com/) y selecciona el proyecto que está asociado a `app/google-services.json`.
2. En **Build > AI Logic**, presiona **Get started** y selecciona **Gemini Developer API**, **no** Agent Platform / Vertex AI.
3. Para mantener la cuota gratuita usa el plan **Spark**, sin una cuenta de facturación. Confirma que el modelo elegido tenga cuota disponible para tu proyecto. Las cuotas y disponibilidad pueden cambiar.
4. Configura **Firebase App Check**. En desarrollo se utiliza el proveedor de depuración; copia el token de depuración que genera la app en Logcat y regístralo en la consola de Firebase, en App Check > Manage debug tokens. Para publicar la app se utiliza **Play Integrity**, que también debe registrarse y configurarse en Firebase.
5. Sincroniza Gradle en Android Studio y prueba el chatbot desde el menú. Si falla, revisa App Check, las API habilitadas, la configuración de `google-services.json`, la cuota y Logcat.

No hace falta conectar ChatGPT con Firebase para cambiar el código del repositorio. Para activar servicios o cambiar la configuración del proyecto Firebase sí es necesario entrar a la consola con permisos.

## Comprobar el proyecto

- Abrir sesión, crear una elección, agregar candidatos y registrar votos; revisar que siguen funcionando las notificaciones.
- Entrar en **Voz a texto**, dictar una frase, editarla y exportar el archivo `.txt`.
- Entrar en **Asistente electoral con IA** y hacer dos preguntas sucesivas.
- Ir a **Votaciones**, pulsar **Exportar PDF**, guardar el archivo y comprobar el total y los candidatos.
- Verificar que ya no aparezca la sección **Pruebas de la evaluación** ni sus botones de provocar un cierre.
- Compilar con **JDK 17**, SDK Android 36, Gradle 8.11.1, AGP 8.10.1, Kotlin 2.4.21.

**Importante:** No se puede dar por verificada la compilación o ejecución en un dispositivo sin correr Android Studio / GitHub Actions. En los servicios gratuitos de IA las consultas pueden estar sujetas a uso de datos según las condiciones de Google. No ingresar información confidencial.

## Archivos técnicos

- `AsistenteActivity.kt`: asistente con Firebase AI Logic.
- `TranscripcionActivity.kt`: reconocimiento de voz y exportación TXT.
- `ReportePdf.kt`: creación de PDF.
- `AppCheckConfiguracion.kt`: proveedores de App Check para debug y release.

Los tests de la aplicación que no pertenecían al panel artificial de evaluación se conservan.
