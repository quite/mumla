package se.lublin.mumla.preference;

import android.os.Bundle;
import se.lublin.mumla.R;

public class AuthenticationSettingsFragment extends MumlaPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_authentication, rootKey);
    }
}
