package com.whitemonkeys.console_agent

import java.util.ArrayDeque

class ChatContext(
    private val summarizer: suspend (List<Message>) -> String,
    private val maxMessagesBeforeSummary: Int = 10
) {
    private val history = ArrayDeque<Message>()
    private var summary: String? = null

    suspend fun addMessage(role: String, content: String) {
        history.addLast(Message(role, content))
        if (history.size >= maxMessagesBeforeSummary) {
            summarize()
        }
    }

    private suspend fun summarize() {
        val messagesToSummarize = history.toList()
        val newSummary = summarizer(messagesToSummarize)
        history.clear()
        summary = newSummary
    }

    fun getFullContext(): List<Message> {
        val result = mutableListOf<Message>()
        if (!summary.isNullOrBlank()) {
            result.add(Message("system", "Краткое содержание предыдущей сессии: $summary"))
        }
        result.addAll(history)
        return result
    }

    data class Message(val role: String, val content: String)
}