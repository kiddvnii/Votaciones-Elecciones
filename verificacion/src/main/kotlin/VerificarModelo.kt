import com.google.ai.edge.litertlm.*

// Runs real CPU inference with the exact weights and library version packaged by Android.
// This host smoke test does not replace testing the Android JNI on a physical phone.
fun main(args: Array<String>) {
    Engine(EngineConfig(modelPath = args.single(), backend = Backend.CPU(), maxNumTokens = 2048)).use { engine ->
        engine.initialize()
        engine.createConversation(ConversationConfig(
            systemInstruction = Contents.of("Responde en español y muy brevemente."),
            extraContext = mapOf("enable_thinking" to false), maxOutputToken = 32
        )).use { chat ->
            val respuesta = chat.sendMessage("Di hola en español.").toString().trim()
            check(respuesta.isNotEmpty()) { "El modelo incluido no generó texto" }
            println("INFERENCIA REAL OK: $respuesta")
            val segunda = chat.sendMessage("Ahora dime cuánto es dos más dos.").toString().trim()
            check(segunda.isNotEmpty()) { "Falló la conversación de seguimiento" }
            println("SEGUIMIENTO REAL OK: $segunda")
        }
    }
}
