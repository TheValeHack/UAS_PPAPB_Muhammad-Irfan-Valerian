package com.example.valenote.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.valenote.R
import com.example.valenote.ui.note.NoteActivity

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val noteTitle = inputData.getString("NOTE_TITLE") ?: "Reminder"
        val noteContent = inputData.getString("NOTE_CONTENT") ?: "You have a reminder!"
        val noteId = inputData.getInt("NOTE_ID", -1)

        val intent = Intent(applicationContext, NoteActivity::class.java).apply {
            putExtra("NOTE_ID", noteId) // Kirim ID catatan ke NoteActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            noteId, // Gunakan noteId untuk PendingIntent unik
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, "REMINDER_CHANNEL")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(noteTitle)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(noteId, builder.build())
        }

        return Result.success()
    }
}
