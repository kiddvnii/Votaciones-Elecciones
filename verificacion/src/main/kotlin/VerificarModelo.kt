import com.google.ai.edge.litertlm.*
fun main(args: Array<String>) {
    Engine(EngineConfig(modelPath = args.single(), backend = Backend.CPU(), maxNumTokens = 2048)).use { engine ->
        engine.initialize()
        engine.createConversation(ConversationConfig(
            systemInstruction = Contents.of("Responde en español y muy brevemente."),
            extraContext = mapOf("enable_thinking" to false),
        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0), maxOutputToken = 32
        )).use { chat ->
            val respuesta = chat.sendMessage("Di hola en español.").toString().trim()
            check(respuesta.isNotEmpty()) { "El modelo incluido no generó texto" }
            println("INFERENCIA REAL OK: $respuesta")
            val segunda = chat.sendMessage("Ahora dime cuánto es dos más dos.").toString().trim()
            check(segunda.isNotEmpty()) { "Falló la conversación de seguimiento" }
            println("RESPUESTA DE SEGUIMIENTO: $segunda")
            check(Regex("(?i)(\\b4\\b|\\bcuatro\\b)").containsMatchIn(segunda)) {
                "El modelo no respondió correctamente a la comprobación básica de seguimiento"
            }
            println("SEGUIMIENTO REAL Y COMPROBACION BASICA OK")
        }
    }
}
