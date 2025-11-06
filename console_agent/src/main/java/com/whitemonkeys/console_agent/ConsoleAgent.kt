package com.whitemonkeys.console_agent

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import kotlinx.coroutines.runBlocking

fun main() {

    val deepseekApiKey = "sk-dcab13bf16de4632a4bdc7e158e1bcbe"

    val deepseekClient = DeepSeekLLMClient(
        apiKey = deepseekApiKey,
    )

    val basePrompt = prompt("base-prompt") {
        system("Ты — полезный русскоязычный ассистент. Отвечай кратко и по делу.")
    }

    val request = prompt(basePrompt) {
        user("Сгенерируй одно краткое приветствие пользователю")
    }

    runBlocking {
        val response = deepseekClient.execute(prompt = request, model = DeepSeekModels.DeepSeekChat)
        println(response) // Полученный ответ от модели
    }
}