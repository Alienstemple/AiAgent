package com.whitemonkeys.console_agent

import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import java.util.*
import java.io.File
import java.io.FileInputStream

/**
 * Функция для чтения многострочного ввода из консоли.
 * Пользователь вводит строки, завершение ввода - пустая строка или команда .end
 * @return Собранный текст или null, если введена команда .exit
 */
fun readMultilineInput(scanner: Scanner): String? {
    val lines = mutableListOf<String>()
    
    while (true) {
        if (!scanner.hasNextLine()) {
            return if (lines.isEmpty()) null else lines.joinToString("\n")
        }
        
        val line = scanner.nextLine()
        
        // Проверяем команды выхода
        if (line == ".exit") {
            return ".exit"
        }
        
        // Пустая строка или команда .end завершают ввод
        if (line.isBlank() || line == ".end") {
            break
        }
        
        lines.add(line)
    }
    
    return if (lines.isEmpty()) "" else lines.joinToString("\n")
}

fun main() = runBlocking {
    // Загружаем секреты из файла
    val secretsFile = File("console_agent/secrets.properties")
    if (!secretsFile.exists()) {
        println("Ошибка: файл secrets.properties не найден!")
        println("Создайте файл console_agent/secrets.properties со следующим содержимым:")
        println("apiKey=ваш_api_key")
        println("folderId=ваш_folder_id")
        return@runBlocking
    }
    
    val properties = Properties()
    FileInputStream(secretsFile).use { properties.load(it) }
    
    val apiKey = properties.getProperty("apiKey")
        ?: throw IllegalStateException("apiKey не найден в secrets.properties")
    val folderId = properties.getProperty("folderId")
        ?: throw IllegalStateException("folderId не найден в secrets.properties")
    
    val gptClient = YandexGptClient(apiKey, folderId)

    val scanner = Scanner(System.`in`)
    println("=== Консольный чат-бот с Yandex GPT ===")
    println("Введите ваше сообщение (многострочный ввод поддерживается).")
    println("Для завершения ввода введите пустую строку или '.end'")
    println("Для выхода введите '.exit'")

    while (true) {
        print("Вы: ")
        val userInput = readMultilineInput(scanner)

        // Условие выхода из цикла
        if (userInput == null || userInput == ".exit") {
            println("До свидания!")
            break
        }

        if (userInput.isBlank()) {
            continue
        }

        try {
            // Отправляем запрос и получаем ответ
            val botResponse = gptClient.sendMessage(userInput)
            println("Бот: $botResponse")
        } catch (e: Exception) {
            println("Произошла ошибка: ${e.message}")
        }
        println() // Печатаем пустую строку для читабельности
    }
}

class YandexGptClient(
    private val apiKey: String,
    private val folderId: String
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    // Функция для отправки сообщения и получения ответа
    suspend fun sendMessage(userMessage: String): String {
        // Формируем полный URI модели, включая folderId
        val modelUri = "gpt://$folderId/yandexgpt-lite"

        // Создаем запрос
        val request = YandexGptRequest(
            modelUri = modelUri,
            completionOptions = YandexGptRequest.CompletionOptions(
                temperature = 0.7,
                maxTokens = 3000
            ),
            messages = listOf(
                YandexGptRequest.Message(
                    role = "user",
                    text = userMessage
                )
            )
        )

        // Отправляем POST-запрос
        val response: YandexGptResponse = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
            header("Authorization", "Api-Key $apiKey")
            header("Content-Type", "application/json")
            setBody(request)
        }.body()

        // Извлекаем и возвращаем текст ответа
        return response.result.alternatives.first().message.text
    }
}

@Serializable
data class YandexGptRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>
) {
    @Serializable
    data class CompletionOptions(
        val stream: Boolean = false,
        val temperature: Double = 0.5,
        val maxTokens: Int = 1000
    )

    @Serializable
    data class Message(
        val role: String, // "user" или "assistant"
        val text: String
    )
}

// Ответ от Yandex GPT
@Serializable
data class YandexGptResponse(
    val result: Result
) {
    @Serializable
    data class Result(
        val alternatives: List<Alternative>,
        val usage: Usage
    ) {
        @Serializable
        data class Alternative(
            val message: Message,
            val status: String
        ) {
            @Serializable
            data class Message(
                val role: String,
                val text: String
            )
        }

        @Serializable
        data class Usage(
            val inputTextTokens: String,
            val completionTokens: String,
            val totalTokens: String
        )
    }
}
