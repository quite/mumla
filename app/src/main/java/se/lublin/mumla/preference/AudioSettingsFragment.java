package se.lublin.mumla.preference;

import android.os.Bundle;
import se.lublin.mumla.R;

public class AudioSettingsFragment extends MumlaPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_audio, rootKey);
        PreferenceHelpers.configureAudioPreferences(getPreferenceScreen());
    }
}
