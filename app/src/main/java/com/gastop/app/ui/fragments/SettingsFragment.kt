package com.gastop.app.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.gastop.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
