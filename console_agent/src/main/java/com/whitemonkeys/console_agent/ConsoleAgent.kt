package com.whitemonkeys.console_agent

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LlmGreetingResponse(
    val greeting: String
)

fun main() {

    val deepseekApiKey = "sk-dcab13bf16de4632a4bdc7e158e1bcbe"

    val deepseekClient = DeepSeekLLMClient(
        apiKey = deepseekApiKey,
    )

    val basePrompt = prompt("base-prompt") {
        system("Ты — полезный русскоязычный ассистент. Отвечай кратко и по делу.")
    }

    val request = prompt(basePrompt) {
        user("""
        Сгенерируй одно краткое приветствие пользователю.
        Ответ должен быть строго в формате JSON с полем "greeting", например: {"greeting": "Привет!"}
    """.trimIndent())
    }

    runBlocking {
        val response = deepseekClient.execute(prompt = request, model = DeepSeekModels.DeepSeekChat)
        println(response) // Полученный ответ от модели
        response.forEach { messageResponse ->
            val cleanJson = messageResponse.content.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            try {
                val parsed = Json.decodeFromString<LlmGreetingResponse>(cleanJson)
                println("Получено приветствие: ${parsed.greeting}")
            } catch (e: Exception) {
                println("Ошибка парсинга JSON: $e")
                println("Сырой ответ: $response")
            }
        }
    }
}