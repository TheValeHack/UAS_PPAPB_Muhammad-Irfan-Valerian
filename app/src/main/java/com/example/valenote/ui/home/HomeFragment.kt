package com.example.valenote.ui.home

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.valenote.R
import com.example.valenote.api.ApiClient
import com.example.valenote.database.AppDatabase
import com.example.valenote.databinding.FragmentHomeBinding
import com.example.valenote.repository.NoteRepository
import com.example.valenote.ui.note.NoteActivity
import com.example.valenote.ui.note.NoteViewModel
import com.example.valenote.ui.note.NoteViewModelFactory
import com.example.valenote.utils.SharedPreferencesHelper
import com.example.valenote.utils.SpaceItemDecoration
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var noteRepository: NoteRepository
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel
    private var isMultiSelectMode = false


    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            visibility = View.GONE
            (requireActivity() as AppCompatActivity).setSupportActionBar(this)
            setTitleTextColor(ContextCompat.getColor(context, android.R.color.white))
            overflowIcon?.setTint(ContextCompat.getColor(context, android.R.color.white))
            navigationIcon?.setTint(ContextCompat.getColor(context, android.R.color.white))
        }

        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)

                menu.findItem(R.id.action_delete)?.isVisible = isMultiSelectMode
                menu.findItem(R.id.action_pin)?.isVisible = isMultiSelectMode

                for (i in 0 until menu.size()) {
                    val menuItem = menu.getItem(i)
                    menuItem.icon?.setTint(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete -> {
                        handleDeleteSelectedNotes()
                        true
                    }
                    R.id.action_pin -> {
                        handlePinSelectedNotes()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        val noteDao = AppDatabase.getDatabase(requireContext()).noteDao()
        noteRepository = NoteRepository(noteDao, ApiClient.apiService)

        noteAdapter = NoteAdapter(
            onClick = { note ->
                if (isMultiSelectMode) {
                    toggleSelection(note.id)
                } else {
                    val intent = Intent(requireContext(), NoteActivity::class.java)
                    intent.putExtra("NOTE_ID", note.id)
                    startActivity(intent)
                }
            },
            onLongPress = { noteId -> toggleMultiSelect(noteId) },
            toggleSelection = { noteId -> toggleSelection(noteId) }
        )
        val repository = NoteRepository(noteDao, ApiClient.apiService)
        val factory = NoteViewModelFactory(repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        lifecycleScope.launch {
            noteViewModel.syncAllNotes { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Sinkronisasi berhasil", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Sinkronisasi gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }

        with(binding){
            btnCreate.setOnClickListener {
                val redirectToNote = Intent(requireContext(), NoteActivity::class.java)
                startActivity(redirectToNote)
            }

            recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())
            recyclerViewNotes.adapter = noteAdapter
            recyclerViewNotes.addItemDecoration(SpaceItemDecoration(12.toPx()))

            val preferences = SharedPreferencesHelper(requireContext())
            val userId = preferences.getUserId()


            etSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s.toString()
                    if (query.isNotEmpty()) {
                        noteViewModel.searchNotes(userId, query).observe(viewLifecycleOwner) { notes ->
                            noteAdapter.submitList(notes)
                        }
                    } else {
                        loadNotes()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            loadNotes()
        }
    }
    private fun loadNotes() {
        val preferences = SharedPreferencesHelper(requireContext())
        lifecycleScope.launch {
            noteRepository.getAllNotesForUser(preferences.getUserId()).observe(viewLifecycleOwner) { notes ->
                val pinnedNotes = notes.filter { it.isPinned }
                val unpinnedNotes = notes.filter { !it.isPinned }
                noteAdapter.submitList(pinnedNotes + unpinnedNotes)
            }
        }
    }
    private fun toggleMultiSelect(noteId: Int) {
        isMultiSelectMode = true
        binding.toolbar.visibility = View.VISIBLE
        requireActivity().invalidateOptionsMenu()
        noteAdapter.toggleNoteSelection(noteId)
    }

    private fun toggleSelection(noteId: Int) {
        noteAdapter.toggleNoteSelection(noteId)
        if (noteAdapter.getSelectedNotes().isEmpty()) {
            isMultiSelectMode = false
            binding.toolbar.visibility = View.GONE
            requireActivity().invalidateOptionsMenu()
        }
    }
    private fun handleDeleteSelectedNotes() {
        val selectedNotes = noteAdapter.getSelectedNotes()
        Log.d("HomeFragment", "handleDeleteSelectedNotes 1: $selectedNotes")
        if (selectedNotes.isNotEmpty()) {
            lifecycleScope.launch {
                Log.d("HomeFragment", "handleDeleteSelectedNotes 2: $selectedNotes")
                noteViewModel.deleteNotes(selectedNotes)
                noteAdapter.clearSelection()
                isMultiSelectMode = false
                binding.toolbar.visibility = View.GONE
                requireActivity().invalidateOptionsMenu()
                Toast.makeText(requireContext(), "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Tidak ada catatan yang dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePinSelectedNotes() {
        val selectedNotes = noteAdapter.getSelectedNotes()
        Log.d("HomeFragment", "handlePinSelectedNotes 1: $selectedNotes")
        if (selectedNotes.isNotEmpty()) {
            Log.d("HomeFragment", "handlePinSelectedNotes 2: $selectedNotes")
            lifecycleScope.launch {
                Log.d("HomeFragment", "handlePinSelectedNotes 3: $selectedNotes")
                noteViewModel.pinNotes(selectedNotes)
                noteAdapter.clearSelection()
                isMultiSelectMode = false
                binding.toolbar.visibility = View.GONE
                requireActivity().invalidateOptionsMenu()
                Toast.makeText(requireContext(), "Catatan berhasil dipin", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Tidak ada catatan yang dipilih", Toast.LENGTH_SHORT).show()
        }
    }
    fun Int.toPx(): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (this * density).toInt()
    }
}