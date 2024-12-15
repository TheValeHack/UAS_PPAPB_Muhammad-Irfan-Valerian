package com.example.valenote.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.valenote.R

class InnerSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
