package com.whitemonkeys.mcp_client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Map<String, @Serializable(with = JsonObjectSerializer::class) Any> = emptyMap(),
    val id: String
)

@Serializable
data class JsonRpcResponse<T>(
    val jsonrpc: String,
    val id: String,
    val result: T? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonObject? = null
)

// Сериализатор для произвольных значений в params (упрощённый)
object JsonObjectSerializer : KSerializer<Any> {
    override val descriptor = JsonObject.serializer().descriptor
    override fun serialize(encoder: Encoder, value: Any) {
        val json = Json.encodeToJsonElement(value)
        encoder.encodeSerializableValue(JsonObject.serializer(), json as JsonObject)
    }
    override fun deserialize(decoder: Decoder): Any {
        return decoder.decodeSerializableValue(JsonObject.serializer())
    }
}

// Расширение для удобной сериализации любого объекта
inline fun <reified T> Json.encodeToJsonElement(value: T): JsonElement =
    encodeToJsonElement(serializersModule.serializer(), value)

// Главная функция
suspend fun main() {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    val mcpUrl = "http://localhost:8080"

    val request = JsonRpcRequest(
        method = "mcp/listTools",
        id = "list-tools-${System.currentTimeMillis()}"
    )

    try {
        val response: JsonRpcResponse<JsonArray> = client.post(mcpUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        if (response.error != null) {
            println("❌ Ошибка MCP: [${response.error.code}] ${response.error.message}")
            return
        }

        println("✅ Получено ${response.result?.size ?: 0} инструментов:")
        response.result?.forEach { tool ->
            if (tool is JsonObject) {
                val name = tool["name"]?.jsonPrimitive?.content ?: "неизвестно"
                val description = tool["description"]?.jsonPrimitive?.content ?: ""
                println("- $name: $description")
            }
        }

    } catch (e: Exception) {
        println("💥 Ошибка подключения: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}