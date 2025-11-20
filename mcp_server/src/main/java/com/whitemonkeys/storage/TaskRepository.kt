package com.whitemonkeys.storage

import com.whitemonkeys.model.Task
import kotlinx.serialization.json.Json
import java.io.File

object TaskRepository {
    private const val FILE = "tasks.json"
    private val json = Json { prettyPrint = true }

    fun getAll(): List<Task> = File(FILE).takeIf { it.exists() }
        ?.readText()
        ?.let { if (it.isBlank()) emptyList() else json.decodeFromString(it) }
        ?: emptyList()

    fun save(tasks: List<Task>) {
        File(FILE).writeText(json.encodeToString(tasks))
    }
}