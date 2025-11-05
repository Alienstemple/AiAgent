package com.whitemonkeys.aiagent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.ext.agent.singleRunStrategy
import ai.koog.agents.local.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.llm.LLModel
import android.annotation.SuppressLint
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/*
 A simple assistant that can access the web and help you automate web related tasks, e.g. market or competitor research.
*/

//region JSON instance
@OptIn(ExperimentalSerializationApi::class)
private val json =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
        namingStrategy = JsonNamingStrategy.SnakeCase
    }
//endregion

//region Web search DTOs
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WebSearchResult(
    val organic: List<OrganicResult>,
) {
    @Serializable
    data class OrganicResult(
        val link: String,
        val title: String,
        val description: String,
        val rank: Int,
        val globalRank: Int,
    )
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WebPageScrapingResult(
    val body: String, // will be a markdown version of the page
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BrightDataRequest(
    val zone: String,
    val url: String,
    val format: String,
    val dataFormat: String? = null,
)
//endregion

//region Web search tools
class WebSearchTools(
    private val brightDataKey: String,
) : ToolSet {
    //region HTTP client
    private val httpClient =
        HttpClient {
            defaultRequest {
                url("https://api.brightdata.com/request")
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $brightDataKey")
            }

            install(ContentNegotiation) {
                json(json)
            }
        }
    //endregion

    //region Search tool
    @Tool
    @LLMDescription("Search for a query on Google.")
    @Suppress("unused")
    suspend fun search(
        @LLMDescription("The query to search")
        query: String,
    ): WebSearchResult {
        val url =
            URLBuilder("https://www.google.com/search")
                .apply {
                    parameters.append("brd_json", "1")
                    parameters.append("q", query)
                }.buildString()

        val request =
            BrightDataRequest(
                zone = "serp_api1",
                url = url,
                format = "raw",
            )

        val response =
            httpClient
                .post {
                    setBody(request)
                }

        return response.body<WebSearchResult>()
    }
    //endregion

    //region Scrape tool
    @Tool
    @LLMDescription("Scrape a web page for content")
    @Suppress("unused")
    suspend fun scrape(
        @LLMDescription("The URL to scrape") url: String,
    ): WebPageScrapingResult {
        val request = BrightDataRequest(
            zone = "web_unlocker1",
            url = url,
            format = "json",
            dataFormat = "markdown",
        )
        val response = httpClient.post { setBody(request) }
        val result = response.body<WebPageScrapingResult>()

        // Optional: limit content length to avoid API overflow
        val maxChars = 50_000 // ~12k tokens
        val safeContent = if (result.body.length > maxChars) {
            result.copy(body = result.body.take(maxChars) + "\n\n[Content truncated]")
        } else {
            result
        }

        return safeContent
    }
}
//endregion

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



