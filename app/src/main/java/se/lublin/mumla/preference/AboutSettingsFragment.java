package se.lublin.mumla.preference;

import static java.util.Objects.requireNonNull;
import static se.lublin.mumla.app.DialogUtils.showAllNewsDialog;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import se.lublin.mumla.BuildConfig;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;

public class AboutSettingsFragment extends MumlaPreferenceFragment {
    private static final String VERSION_KEY = "version";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_about, rootKey);

        String summary = String.format("%s (code %s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        if (BuildConfig.FLAVOR.equals("foss")) {
            summary += "\nFOSS flavor";
        } else if (BuildConfig.FLAVOR.equals("beta")) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            summary += String.format("\nBeta flavor with versioncode %s", BuildConfig.VERSION_CODE);
            summary += String.format("\nBuildtime %s UTC", df.format(new Date(BuildConfig.TIMESTAMP)));
        } else if (BuildConfig.FLAVOR.equals("donation")) {
            summary += String.format("\n*) %s", getString(R.string.donation_thanks));
        }
        Preference versionPreference = getPreferenceScreen().findPreference(VERSION_KEY);
        requireNonNull(versionPreference).setSummary(summary);
        requireNonNull(versionPreference).setOnPreferenceClickListener(preference -> {
            Settings.getInstance(requireContext()).resetNewsShownVersion();
            return true;
        });
        Preference showNewsPreference = getPreferenceScreen().findPreference("showNews");
        requireNonNull(showNewsPreference).setOnPreferenceClickListener(preference -> {
            showAllNewsDialog(requireContext());
            return true;
        });
    }
}
