package com.whitemonkeys.aiagent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.prompt.DashscopeLLMClient
import ai.koog.agents.prompt.DashscopeModels
import ai.koog.agents.prompt.PromptExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QwenService(private val apiKey: String) {
    private val dashscopeClient = DashscopeLLMClient(apiKey = apiKey)
    private val promptExecutor = PromptExecutor(dashscopeClient)
    private val agent = AIAgent(
        promptExecutor = promptExecutor,
        systemPrompt = "You are a helpful assistant. Answer user questions concisely and clearly.",
        llmModel = DashscopeModels.QWEN_PLUS
    )

    suspend fun sendMessage(message: String): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                val response = agent.run(message)
                Result.success(response)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

