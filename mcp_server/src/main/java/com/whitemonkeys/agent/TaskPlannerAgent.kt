package com.whitemonkeys.agent

import com.whitemonkeys.model.Task
import com.whitemonkeys.storage.TaskRepository
import java.io.File
import java.time.LocalDate

class TaskPlannerAgent {
    private val tasks = TaskRepository.getAll().toMutableList()

    init {
        // Загрузка задач при инициализации
        tasks.clear()
        tasks.addAll(TaskRepository.getAll())
        
        // Ежедневный summary — через реактивное поведение
        val lastRunFile = File("last_run.txt")
        val today = LocalDate.now().toString()
        if (!lastRunFile.exists() || lastRunFile.readText().trim() != today) {
            lastRunFile.writeText(today)
            val pending = tasks.filter { !it.completed }
            val summary = "📅 **Daily Summary**\n" +
                    "✅ Completed: ${tasks.count { it.completed }}\n" +
                    "⏳ Pending: ${pending.size}\n" +
                    pending.take(3).joinToString("\n") { "- ${it.title}" }
            println(summary)
        }
    }

    // Методы для работы с задачами
    fun addTask(title: String, description: String? = null, dueDate: String? = null): String {
        tasks.add(Task(title = title, description = description, dueDate = dueDate))
        TaskRepository.save(tasks)
        return "✅ Task '$title' added."
    }

    fun listTasks(completed: Boolean? = null): String {
        val filtered = if (completed != null)
            tasks.filter { it.completed == completed }
        else tasks

        return filtered.joinToString("\n") { task ->
            val mark = if (task.completed) "✅" else "⏳"
            "$mark [${task.id.take(8)}] ${task.title} ${task.dueDate?.let { "(до $it)" } ?: ""}"
        }.ifEmpty { "No tasks." }
    }

    fun markCompleted(id: String): String {
        // Поиск по полному ID или по первым 8 символам
        val task = tasks.find { 
            it.id == id || it.id.take(8) == id.take(8)
        }
        if (task != null && !task.completed) {
            tasks[tasks.indexOf(task)] = task.copy(completed = true)
            TaskRepository.save(tasks)
            return "✅ Task '${task.title}' marked as completed."
        } else if (task != null && task.completed) {
            return "❌ Task '${task.title}' is already completed."
        } else {
            return "❌ Task not found. Use ID from 'list' command."
        }
    }

    // Основной цикл агента — ожидание команд от пользователя
    suspend fun run() {
        while (true) {
            print("TaskPlanner> ")
            val input = readLine() ?: break
            if (input == "exit") break
            if (input.isBlank()) continue

            // Простая обработка команд
            val response = processCommand(input)
            println(response)
        }
    }

    private fun processCommand(input: String): String {
        return when {
            input.startsWith("add ") -> {
                val parts = input.removePrefix("add ").split("|")
                val title = parts.getOrNull(0)?.trim() ?: return "❌ Title required"
                val desc = parts.getOrNull(1)?.trim()
                val due = parts.getOrNull(2)?.trim()
                addTask(title, desc, due)
            }
            input.startsWith("list") -> {
                val filter = when {
                    input.contains("completed") -> true
                    input.contains("pending") -> false
                    else -> null
                }
                listTasks(filter)
            }
            input.startsWith("complete ") -> {
                val id = input.removePrefix("complete ").trim()
                markCompleted(id)
            }
            else -> {
                """
                Unknown command. Available commands:
                - add <title>|description|dueDate  - Add a new task
                  Example: add Купить молоко|Молоко 3.2%|2024-12-25
                - list                              - List all tasks
                - list completed                    - List only completed tasks
                - list pending                      - List only pending tasks
                - complete <id>                     - Mark task as completed (use ID from list)
                  Example: complete abc12345
                - exit                              - Exit the agent
                """.trimIndent()
            }
        }
    }
}