package com.whitemonkeys.console_agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GigaChatTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Int
)

@Serializable
data class GigaChatRequest(
    val model: String = "GigaChat",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 3000,
    val stream: Boolean = false
) {
    @Serializable
    data class Message(
        val role: String, // "user", "assistant", "system"
        val content: String
    )
}

@Serializable
data class GigaChatResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        val message: Message
    ) {
        @Serializable
        data class Message(
            val role: String,
            val content: String
        )
    }
}