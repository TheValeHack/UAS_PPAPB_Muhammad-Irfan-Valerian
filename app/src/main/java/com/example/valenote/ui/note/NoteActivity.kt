package com.example.valenote.ui.note

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import com.example.valenote.R
import com.example.valenote.api.ApiClient
import com.example.valenote.database.AppDatabase
import com.example.valenote.database.models.Note
import com.example.valenote.databinding.ActivityNoteBinding
import com.example.valenote.repository.NoteRepository
import com.example.valenote.utils.SharedPreferencesHelper
import com.example.valenote.utils.ReminderWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteBinding
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var preferences: SharedPreferencesHelper
    private var reminderTimestamp: Long? = null
    private var currentNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val additionalPadding = 24.toPx() // Konversi 24dp ke px
            v.setPadding(
                systemBars.left + additionalPadding,
                systemBars.top + additionalPadding,
                systemBars.right + additionalPadding,
                systemBars.bottom + additionalPadding
            )
            insets
        }

        preferences = SharedPreferencesHelper(this)

        val noteDao = AppDatabase.getDatabase(this).noteDao()
        val repository = NoteRepository(noteDao, ApiClient.apiService)
        val factory = NoteViewModelFactory(repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        setupUI()
    }

    private fun setupUI() {
        val noteId = intent.getIntExtra("NOTE_ID", -1)

        with(binding) {
            btnBack.setOnClickListener {
                finish()
            }

            btnSave.setOnClickListener {
                saveNote()
            }

            btnOptions.setOnClickListener {
                showOptionsMenu(it)
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            tvLastModified.text = currentDate

            if (noteId != -1) {
                noteViewModel.getNoteById(noteId).observe(this@NoteActivity) { note ->
                    note?.let {
                        currentNote = it
                        etTitle.setText(it.title)
                        etContent.setText(it.content)
                        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                        tvLastModified.text = dateFormat.format(it.updatedAt)
                        reminderTimestamp = it.reminderAt?.time
                        updateReminderUI()
                        updateSyncStatus(it.isSynced)
                    }
                }
            }
        }

        createNotificationChannel()
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        if (title.isBlank()) {
            Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val note = currentNote?.copy(
            title = title,
            content = content,
            updatedAt = Date(),
            reminderAt = reminderTimestamp?.let { Date(it) }
        ) ?: Note(
            userId = preferences.getUserId(),
            title = title,
            content = content,
            createdAt = Date(),
            updatedAt = Date(),
            reminderAt = reminderTimestamp?.let { Date(it) }
        )

        if (currentNote == null) {
            noteViewModel.addNote(note)
            Toast.makeText(this, "Catatan berhasil disimpan", Toast.LENGTH_SHORT).show()
        } else {
            noteViewModel.updateNote(note)
            Toast.makeText(this, "Catatan berhasil diperbarui", Toast.LENGTH_SHORT).show()
        }

        reminderTimestamp?.let {
            checkNotificationPermission()
            scheduleReminder(note.id, title, content, it)
        }

        finish()
    }

    private fun showOptionsMenu(anchor: android.view.View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_note_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_reminder -> {
                    showDatePicker()
                    true
                }
                R.id.action_pin -> {
                    togglePinStatus()
                    true
                }
                R.id.action_delete -> {
                    deleteNote()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun togglePinStatus() {
        currentNote?.let {
            val isPinned = it.isPinned.not()
            noteViewModel.updatePinStatus(it.id, isPinned)
            val status = if (isPinned) "dipin" else "diunpin"
            Toast.makeText(this, "Catatan berhasil $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteNote() {
        currentNote?.let {
            noteViewModel.deleteNoteById(it.id)
            cancelReminder(it.id)
            Toast.makeText(this, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker(calendar: Calendar) {
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                reminderTimestamp = calendar.timeInMillis
                updateReminderUI()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }

    private fun updateReminderUI() {
        reminderTimestamp?.let {
            val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            binding.tvReminder.text = "Reminder: ${dateFormat.format(Date(it))}"
        }
    }
    private fun updateSyncStatus(isSynced: Boolean) {
        with(binding) {
            syncStatus.backgroundTintList =
                getColorStateList(if (isSynced) R.color.green_200 else R.color.red_200)
            tvSync.setTextColor(getColor(if (isSynced) R.color.green_700 else R.color.red_700))
            tvSync.text = if (isSynced) "Synced" else "UnSynced"
        }
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun scheduleReminder(noteId: Int, title: String, content: String, reminderTime: Long) {
        val delay = reminderTime - System.currentTimeMillis()

        if (delay > 0) {
            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putInt("NOTE_ID", noteId)
                        .putString("NOTE_TITLE", "Masih ingat dengan catatan \"$title\"?")
                        .putString("NOTE_CONTENT", "Sudah waktunya untuk melanjutkan catatanmu tersebut!")
                        .build()
                )
                .addTag("Reminder_$noteId")
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)
        }
    }

    private fun cancelReminder(noteId: Int) {
        WorkManager.getInstance(this).cancelAllWorkByTag("Reminder_$noteId")
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "REMINDER_CHANNEL",
                "Reminder Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for reminder notifications"
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun Int.toPx(): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (this * density).toInt()
    }
}
