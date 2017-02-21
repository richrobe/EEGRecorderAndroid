package media.mit.edu.eegmonitor.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.google.common.net.InternetDomainName;

import media.mit.edu.eegmonitor.R;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_settings);
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // add SettingsFragment to Activity
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupSimplePreferencesScreen();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.unregisterOnSharedPreferenceChangeListener(this);
        }

        private void setupSimplePreferencesScreen() {
            addPreferencesFromResource(R.xml.prefs);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String address = sp.getString(getString(R.string.pref_address), "");
            findPreference(getString(R.string.pref_address)).setSummary(getString(R.string.pref_summary_address, address));
            String port = sp.getString(getString(R.string.pref_port), "");
            findPreference(getString(R.string.pref_port)).setSummary(getString(R.string.pref_summary_port, port));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            if (getString(R.string.pref_enable_streaming).equals(key)) {
                if (((CheckBoxPreference) getPreferenceScreen().findPreference(getString(R.string.pref_enable_streaming))).isChecked()) {
                    getPreferenceScreen().findPreference(getString(R.string.pref_address)).setEnabled(true);
                    getPreferenceScreen().findPreference(getString(R.string.pref_port)).setEnabled(true);
                } else {
                    getPreferenceScreen().findPreference(getString(R.string.pref_address)).setEnabled(false);
                    getPreferenceScreen().findPreference(getString(R.string.pref_port)).setEnabled(false);
                }
            } else if (getString(R.string.pref_address).equals(key)) {
                String address = ((EditTextPreference) getPreferenceScreen().findPreference(getString(R.string.pref_address))).getText();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if (InternetDomainName.isValid(address)) {
                    sp.edit().putString(getString(R.string.pref_address), address).apply();
                    findPreference(getString(R.string.pref_address)).setSummary(getString(R.string.pref_summary_address, address));
                }

            } else if (getString(R.string.pref_port).equals(key)) {
                String port = ((EditTextPreference) getPreferenceScreen().findPreference(getString(R.string.pref_port))).getText();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sp.edit().putString(getString(R.string.pref_port), port).apply();
                findPreference(getString(R.string.pref_port)).setSummary(getString(R.string.pref_summary_port, port));
            }
        }
    }
}
