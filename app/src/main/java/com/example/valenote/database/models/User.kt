package com.example.valenote.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = false) val _id: String,
    val username: String,
    val email: String,
    val password: String,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
