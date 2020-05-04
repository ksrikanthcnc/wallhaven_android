package com.wallhaven

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*

//todo show
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            val preferenceScreen: PreferenceScreen = this.preferenceScreen
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preferenceGroup: PreferenceGroup = preferenceScreen.getPreference(i) as PreferenceGroup
                for (j in 0 until preferenceGroup.preferenceCount) {
                    when (val preference: Preference = preferenceGroup.getPreference(j)) {
                        is EditTextPreference -> preference.summary = preference.text
                        is MultiSelectListPreference -> preference.summary = preference.values?.toString() ?: ""
                        is ListPreference -> preference.summary = preference.value?.toString() ?: ""
                        is SeekBarPreference -> preference.summary = preference.value.toString()
                    }
                }
            }
        }
    }
}