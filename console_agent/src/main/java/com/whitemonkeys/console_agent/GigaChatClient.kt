package com.whitemonkeys.console_agent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import java.util.*

class GigaChatClient(
    private val clientId: String,
    private val clientSecret: String
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            Json { ignoreUnknownKeys = true }
        }
    }

    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

    private suspend fun getAccessToken(): String {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken!!
        }

        val credentials = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val rqUid = UUID.randomUUID().toString()

        val response: GigaChatTokenResponse = httpClient.post("https://ngw.devices.sberbank.ru:9443/api/v2/oauth") {
            header("RqUID", rqUid)
            header("Authorization", "Basic $credentials")
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody("scope=GIGACHAT_API_PUB")
        }.body()

        accessToken = response.accessToken
        tokenExpiry = System.currentTimeMillis() + (response.expiresIn.toLong() * 1000) - 60_000 // обновляем за минуту до истечения
        return accessToken!!
    }

    suspend fun sendMessage(userMessage: String): String {
        val token = getAccessToken()

        val request = GigaChatRequest(
            messages = listOf(
                GigaChatRequest.Message(role = "user", content = userMessage)
            ),
            temperature = 0.7,
            max_tokens = 2000
        )

        val response: GigaChatResponse = httpClient.post("https://gigachat.devices.sberbank.ru/api/v1/chat/completions") {
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json")
            setBody(request)
        }.body()

        return response.choices.firstOrNull()?.message?.content ?: "No response from GigaChat"
    }
}