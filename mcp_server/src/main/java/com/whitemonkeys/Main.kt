package com.whitemonkeys

import com.whitemonkeys.agent.TaskPlannerAgent
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    TaskPlannerAgent().run()
}