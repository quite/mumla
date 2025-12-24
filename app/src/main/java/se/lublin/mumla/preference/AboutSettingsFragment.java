package se.lublin.mumla.preference;

import static java.util.Objects.requireNonNull;
import static se.lublin.mumla.app.DialogUtils.showNewsDialog;

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

        String details = "";
        if (BuildConfig.FLAVOR.equals("beta")) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            details = String.format("Beta flavor with versioncode %s\nBuildtime %s UTC",
                    BuildConfig.VERSION_CODE, df.format(new Date(BuildConfig.TIMESTAMP)));
        } else if (BuildConfig.FLAVOR.equals("donation")) {
            details = String.format("\n*) %s", getString(R.string.donation_thanks));
        }

        Preference versionPreference = getPreferenceScreen().findPreference(VERSION_KEY);
        requireNonNull(versionPreference).setSummary(String.format("%s\n%s", BuildConfig.VERSION_NAME, details));
        requireNonNull(versionPreference).setOnPreferenceClickListener(preference -> {
            Settings.getInstance(requireContext()).resetNewsShownVersion();
            Toast.makeText(requireContext(), "Latest update news will be shown again on app startup", Toast.LENGTH_LONG).show();
            return true;
        });
        Preference showNewsPreference = getPreferenceScreen().findPreference("showNews");
        requireNonNull(showNewsPreference).setOnPreferenceClickListener(preference -> {
            showNewsDialog(requireContext());
            return true;
        });
    }
}
