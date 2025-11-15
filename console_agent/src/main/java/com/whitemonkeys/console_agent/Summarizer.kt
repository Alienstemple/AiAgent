package com.whitemonkeys.console_agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.util.ArrayDeque

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class ChatMemory(
    val history: List<Message> = emptyList(),
    val summary: String? = null
)

class ChatContext(
    private val summarizer: suspend (List<Message>) -> String,
    private val maxMessagesBeforeSummary: Int = 10,
    private val memoryFile: File = File("chat_memory.json")
) {
    private val history = ArrayDeque<Message>()
    private var summary: String? = null

    init {
        loadMemory()
    }

    private fun loadMemory() {
        try {
            if (memoryFile.exists() && memoryFile.length() > 0) {
                val json = memoryFile.readText()
                val memory = Json.decodeFromString<ChatMemory>(json)
                history.clear()
                history.addAll(memory.history)
                summary = memory.summary
            }
        } catch (e: Exception) {
            println("Предупреждение: не удалось загрузить память из файла: ${e.message}")
            // Продолжаем работу с пустым контекстом
        }
    }

    private fun saveMemory() {
        try {
            val memory = ChatMemory(
                history = history.toList(),
                summary = summary
            )
            val json = Json.encodeToString(memory)
            memoryFile.writeText(json)
        } catch (e: Exception) {
            println("Предупреждение: не удалось сохранить память в файл: ${e.message}")
        }
    }

    suspend fun addMessage(role: String, content: String) {
        history.addLast(Message(role, content))
        saveMemory()
        if (history.size >= maxMessagesBeforeSummary) {
            summarize()
        }
    }

    private suspend fun summarize() {
        val messagesToSummarize = history.toList()
        val newSummary = summarizer(messagesToSummarize)
        history.clear()
        summary = newSummary
        saveMemory()
    }

    fun getFullContext(): List<Message> {
        val result = mutableListOf<Message>()
        if (!summary.isNullOrBlank()) {
            result.add(Message("system", "Краткое содержание предыдущей сессии: $summary"))
        }
        result.addAll(history)
        return result
    }
}