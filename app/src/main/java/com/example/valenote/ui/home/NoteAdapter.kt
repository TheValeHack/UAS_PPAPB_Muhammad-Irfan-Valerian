package com.example.valenote.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.valenote.R
import com.example.valenote.database.models.Note
import com.example.valenote.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongPress: (Int) -> Unit,
    private val toggleSelection: (Int) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private val selectedNotes = mutableSetOf<Int>()

    fun toggleNoteSelection(noteId: Int) {
        val index = currentList.indexOfFirst { it.id == noteId }
        if (index != -1) {
            if (selectedNotes.contains(noteId)) {
                selectedNotes.remove(noteId)
            } else {
                selectedNotes.add(noteId)
            }
            notifyItemChanged(index)
        }
        Log.d("NoteAdapter", "Selected Notes: $selectedNotes")
    }

    fun clearSelection() {
        selectedNotes.clear()
        notifyDataSetChanged()
    }

    fun getSelectedNotes(): Set<Int> = selectedNotes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        val isSelected = selectedNotes.contains(note.id)
        holder.bind(note, isSelected)
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note, isSelected: Boolean) {
            with(binding) {
                tvTitle.text = note.title
                tvContent.text = note.content

                val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                tvDate.text = dateFormat.format(note.updatedAt)

                if (note.isSynced) {
                    syncStatus.backgroundTintList =
                        itemView.context.getColorStateList(R.color.green_200)
                    tvSync.setTextColor(itemView.context.getColor(R.color.green_700))
                    tvSync.text = "Synced"
                } else {
                    syncStatus.backgroundTintList =
                        itemView.context.getColorStateList(R.color.red_200)
                    tvSync.setTextColor(itemView.context.getColor(R.color.red_700))
                    tvSync.text = "UnSynced"
                }

                if (note.reminderAt != null) {
                    reminderStatus.visibility = View.VISIBLE
                    tvReminder.text = "Reminder: ${dateFormat.format(note.reminderAt)}"
                } else {
                    reminderStatus.visibility = View.GONE
                }

                if (note.isPinned) {
                    pinIcon.visibility = View.VISIBLE
                } else {
                    pinIcon.visibility = View.GONE
                }


                main.setBackgroundResource(
                    if (isSelected) R.drawable.selected_note_background else R.drawable.item_note_background
                )


                main.setOnClickListener {
                    if (selectedNotes.isEmpty()) {
                        onClick(note)
                    } else {
                        toggleSelection(note.id)
                    }
                }

                main.setOnLongClickListener {
                    onLongPress(note.id)
                    true
                }
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}

