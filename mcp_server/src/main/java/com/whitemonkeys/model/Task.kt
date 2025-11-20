package com.whitemonkeys.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val dueDate: String? = null, // ISO_LOCAL_DATE
    val completed: Boolean = false,
    val createdAt: String = LocalDate.now().toString()
)