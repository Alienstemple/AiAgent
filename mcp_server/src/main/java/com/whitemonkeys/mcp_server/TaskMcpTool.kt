package com.whitemonkeys.mcp_server

import com.whitemonkeys.model.Task
import com.whitemonkeys.storage.TaskRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class McpTool(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val handler: (JsonObject) -> JsonObject
)

object TaskMcpTool {
    val addTask = McpTool(
        name = "add_task",
        description = "Add a new household task",
        parameters = buildJsonObject {
            put("title", buildJsonObject { put("type", JsonPrimitive("string")) })
            put("description", buildJsonObject { put("type", JsonPrimitive("string")); put("optional", JsonPrimitive(true)) })
            put("dueDate", buildJsonObject { put("type", JsonPrimitive("string")); put("format", JsonPrimitive("date")); put("optional", JsonPrimitive(true)) })
        }
    ) { params ->
        val title = params["title"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("Title required")
        val desc = params["description"]?.jsonPrimitive?.contentOrNull
        val due = params["dueDate"]?.jsonPrimitive?.contentOrNull

        val tasks = TaskRepository.getAll().toMutableList()
        tasks.add(Task(title = title, description = desc, dueDate = due))
        TaskRepository.save(tasks)

        buildJsonObject { put("status", JsonPrimitive("success")); put("message", JsonPrimitive("Task added")) }
    }

    val listTasks = McpTool(
        name = "list_tasks",
        description = "List all tasks (completed or pending)",
        parameters = buildJsonObject {
            put("completed", buildJsonObject { put("type", JsonPrimitive("boolean")); put("optional", JsonPrimitive(true)) })
        }
    ) { params ->
        val filterCompleted = params["completed"]?.jsonPrimitive?.booleanOrNull
        val tasks = TaskRepository.getAll()
            .filter { filterCompleted == null || it.completed == filterCompleted }

        buildJsonObject {
            put("tasks", JsonArray(tasks.map { task ->
                buildJsonObject {
                    put("id", JsonPrimitive(task.id))
                    put("title", JsonPrimitive(task.title))
                    put("completed", JsonPrimitive(task.completed))
                    if (task.dueDate != null) put("dueDate", JsonPrimitive(task.dueDate))
                }
            }))
        }
    }
}