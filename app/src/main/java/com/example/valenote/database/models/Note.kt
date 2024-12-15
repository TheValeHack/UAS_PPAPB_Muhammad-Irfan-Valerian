package com.example.valenote.database.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(tableName = "notes", indices = [Index(value = ["apiId"], unique = true)])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName("_id")
    val apiId: String? = null,
    val userId: String,
    val title: String,
    val content: String?,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val reminderAt: Date? = null
)
