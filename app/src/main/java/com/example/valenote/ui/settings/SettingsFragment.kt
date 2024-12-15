package com.example.valenote.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.example.valenote.R
import com.example.valenote.databinding.FragmentHomeBinding
import com.example.valenote.databinding.FragmentSettingsBinding
import com.example.valenote.ui.MainActivity
import com.example.valenote.ui.login.LoginActivity
import com.example.valenote.utils.SharedPreferencesHelper

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferences: SharedPreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager
            .beginTransaction()
            .replace(R.id.preferences_container, InnerSettingsFragment())
            .commit()
        preferences = SharedPreferencesHelper(requireContext())
        binding.btnLogout.setOnClickListener {
            preferences.clearAll()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
