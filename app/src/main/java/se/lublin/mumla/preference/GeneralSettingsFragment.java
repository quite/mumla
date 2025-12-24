package se.lublin.mumla.preference;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;

import androidx.preference.Preference;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import se.lublin.mumla.R;

public class GeneralSettingsFragment extends MumlaPreferenceFragment {
    private static final String USE_TOR_KEY = "useTor";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_general, rootKey);

        Preference useOrbotPreference = getPreferenceScreen().findPreference(USE_TOR_KEY);
        requireNonNull(useOrbotPreference).setEnabled(OrbotHelper.isOrbotInstalled(requireContext()));
    }
}
