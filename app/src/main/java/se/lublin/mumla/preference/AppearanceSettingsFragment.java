package se.lublin.mumla.preference;

import static java.util.Locale.forLanguageTag;
import static se.lublin.mumla.Settings.PREF_LANGUAGE;

import android.os.Bundle;

import androidx.preference.ListPreference;

import java.util.Locale;

import se.lublin.mumla.R;

public class AppearanceSettingsFragment extends MumlaPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_appearance, rootKey);

        ListPreference languagePreference = findPreference(PREF_LANGUAGE);
        if (languagePreference != null) {
            String[] codes = getResources().getStringArray(R.array.languageValues);
            String[] listNames = new String[codes.length + 1];
            String[] listCodes = new String[codes.length + 1];
            listNames[0] = getString(R.string.language_system);
            listCodes[0] = "system";
            int dest = 1;
            for (String code : codes) {
                Locale locale = forLanguageTag(code);
                listNames[dest] = locale.getDisplayName(locale);
                listCodes[dest] = code;
                dest++;
            }
            languagePreference.setEntries(listNames);
            languagePreference.setEntryValues(listCodes);
        }

    }
}
