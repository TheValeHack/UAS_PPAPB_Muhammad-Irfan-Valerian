package com.example.valenote.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserRequest(
    val username: String,
    val email: String,
    val password: String,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
