package com.whitemonkeys.mcp_server

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class GithubUser(
    val login: String,
    val id: Long,
    val name: String?,
    val bio: String?,
    val public_repos: Int
)

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class ListToolsResponse(
    val tools: List<Tool>
)

@Serializable
data class Tool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class CallToolParams(
    val name: String,
    @SerialName("arguments")
    val arguments: JsonObject
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: String,
    val result: JsonObject? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String
)

@Serializable
data class ToolResult(
    val content: List<ContentPart>
)

@Serializable
data class ContentPart(
    val type: String = "text",
    val text: String? = null
)

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        })
    }
}

val tools = listOf(
    Tool(
        name = "getGithubUser",
        description = "Получить информацию о пользователе GitHub по его логину",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("username", buildJsonObject {
                    put("type", "string")
                    put("description", "Логин пользователя на GitHub")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("username"))))
        }
    )
)

suspend fun handleRequest(request: JsonRpcRequest): JsonRpcResponse {
    return try {
        when (request.method) {
            "mcp/list_tools" -> {
                val result = ListToolsResponse(tools)
                JsonRpcResponse(
                    id = request.id,
                    result = kotlinx.serialization.json.Json.encodeToJsonElement(result).jsonObject
                )
            }

            "mcp/call_tool" -> {
                val params = kotlinx.serialization.json.Json.decodeFromJsonElement<CallToolParams>(request.params!!)
                if (params.name != "getGithubUser") {
                    return JsonRpcResponse(
                        id = request.id,
                        error = JsonRpcError(-32602, "Unknown tool")
                    )
                }
                val username = params.arguments["username"]?.jsonPrimitive?.content
                    ?: return JsonRpcResponse(
                        id = request.id,
                        error = JsonRpcError(-32602, "Missing 'username' argument")
                    )

                val user: GithubUser = httpClient.get("https://api.github.com/users/$username").body()
                val text = """
                    Имя: ${user.name ?: "не указано"}
                    Био: ${user.bio ?: "нет"}
                    Публичных репозиториев: ${user.public_repos}
                """.trimIndent()

                val result = ToolResult(
                    content = listOf(ContentPart(type = "text", text = text))
                )

                JsonRpcResponse(
                    id = request.id,
                    result = kotlinx.serialization.json.Json.encodeToJsonElement(result).jsonObject
                )
            }

            else -> JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(-32601, "Method not found")
            )
        }
    } catch (e: Exception) {
        JsonRpcResponse(
            id = request.id,
            error = JsonRpcError(-32000, "Internal error: ${e.message}")
        )
    }
}

fun main() = runBlocking {
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    while (true) {
        val line = readlnOrNull() ?: break
        if (line.isEmpty()) continue

        try {
            val request = json.decodeFromString<JsonRpcRequest>(line)
            val response = handleRequest(request)
            println(json.encodeToString(response))
        } catch (e: Exception) {
            // В случае ошибки парсинга просто игнорируем или логируем
            System.err.println("Parse error: ${e.message}")
        }
    }
}