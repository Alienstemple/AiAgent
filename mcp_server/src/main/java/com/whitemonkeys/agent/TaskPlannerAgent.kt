package com.whitemonkeys.agent
import com.whitemonkeys.storage.TaskRepository
import ai.koog.agents.core.agent.AIAgent

class TaskPlannerAgent : AIAgent() {
    private val tasks = TaskRepository.getAll().toMutableList()

    init {
        // Регистрация инструментов (tools)
        tool("add_task") {
            description = "Add a new task"
            parameter<String>("title") { required = true }
            parameter<String>("description") { required = false }
            parameter<String>("dueDate") { required = false }

            handler { params ->
                val title = params["title"] as String
                val desc = params["description"] as String?
                val due = params["dueDate"] as String?

                tasks.add(Task(title = title, description = desc, dueDate = due))
                TaskRepository.save(tasks)
                "✅ Task '$title' added."
            }
        }

        tool("list_tasks") {
            description = "List all tasks"
            parameter<Boolean>("completed") { required = false }

            handler { params ->
                val filterCompleted = params["completed"] as Boolean?
                val filtered = if (filterCompleted != null)
                    tasks.filter { it.completed == filterCompleted }
                else tasks

                filtered.joinToString("\n") { task ->
                    val mark = if (task.completed) "✅" else "⏳"
                    "$mark ${task.title} ${task.dueDate?.let { "(до $it)" } ?: ""}"
                }.ifEmpty { "No tasks." }
            }
        }

        tool("mark_completed") {
            description = "Mark task as completed by ID"
            parameter<String>("id") { required = true }

            handler { params ->
                val id = params["id"] as String
                val task = tasks.find { it.id == id }
                if (task != null && !task.completed) {
                    tasks[tasks.indexOf(task)] = task.copy(completed = true)
                    TaskRepository.save(tasks)
                    "✅ Task '${task.title}' marked as completed."
                } else {
                    "❌ Task not found or already completed."
                }
            }
        }

        // Ежедневный summary — через реактивное поведение
        onStartup {
            val lastRunFile = File("last_run.txt")
            val today = LocalDate.now().toString()
            if (!lastRunFile.exists() || lastRunFile.readText().trim() != today) {
                lastRunFile.writeText(today)
                val pending = tasks.filter { !it.completed }
                respond(
                    "📅 **Daily Summary**\n" +
                            "✅ Completed: ${tasks.count { it.completed }}\n" +
                            "⏳ Pending: ${pending.size}\n" +
                            pending.take(3).joinToString("\n") { "- ${it.title}" }
                )
            }
        }
    }

    // Основной цикл агента — ожидание команд от пользователя
    override suspend fun run() {
        while (true) {
            val input = prompt("TaskPlanner> ")
            if (input == "exit") break
            if (input.isBlank()) continue

            // Передаём ввод агенту — Koog сам выберет инструмент или ответит
            val response = process(input)
            println(response)
        }
    }
}