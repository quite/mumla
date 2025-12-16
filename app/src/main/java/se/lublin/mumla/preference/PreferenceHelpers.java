package se.lublin.mumla.preference;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import se.lublin.mumla.BuildConfig;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;

public final class PreferenceHelpers {

    private static final String USE_TOR_KEY = "useTor";
    private static final String VERSION_KEY = "version";
    // preventing instantiation
    private PreferenceHelpers() {
    }

    public static void configureOrbotPreferences(PreferenceScreen screen) {
        Preference useOrbotPreference = screen.findPreference(USE_TOR_KEY);
        requireNonNull(useOrbotPreference).setEnabled(OrbotHelper.isOrbotInstalled(screen.getContext()));
    }

    public static void configureAudioPreferences(final PreferenceScreen screen) {
        ListPreference inputPreference = screen.findPreference(Settings.PREF_INPUT_METHOD);
        requireNonNull(inputPreference).setOnPreferenceChangeListener((preference, newValue) -> {
            updateAudioDependents(screen, (String) newValue);
            return true;
        });

        // Scan each bitrate and determine if the device supports it
        ListPreference inputQualityPreference = screen.findPreference(Settings.PREF_INPUT_RATE);
        String[] bitrateNames = new String[requireNonNull(inputQualityPreference).getEntryValues().length];
        for (int x = 0; x < bitrateNames.length; x++) {
            int bitrate = Integer.parseInt(inputQualityPreference.getEntryValues()[x].toString());
            boolean supported = AudioRecord.getMinBufferSize(bitrate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) > 0;
            bitrateNames[x] = bitrate + "Hz" + (supported ? "" : " (unsupported)");
        }
        inputQualityPreference.setEntries(bitrateNames);

        updateAudioDependents(screen, inputPreference.getValue());
    }

    private static void updateAudioDependents(PreferenceScreen screen, String inputMethod) {
        PreferenceCategory pttCategory = screen.findPreference("ptt_settings");
        PreferenceCategory vadCategory = screen.findPreference("vad_settings");
        requireNonNull(pttCategory).setEnabled(Settings.ARRAY_INPUT_METHOD_PTT.equals(inputMethod));
        requireNonNull(vadCategory).setEnabled(Settings.ARRAY_INPUT_METHOD_VOICE.equals(inputMethod));
    }

    public static void configureAboutPreferences(Context context, PreferenceScreen screen) {
        String version = "Unknown";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = info.versionName;
            if (BuildConfig.FLAVOR.equals("beta")) {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                version += ("\nBeta flavor, versioncode: " + info.versionCode
                        + "\nbuildtime: " + f.format(new Date(BuildConfig.TIMESTAMP)) + " UTC");
            } else if (BuildConfig.FLAVOR.equals("donation")) {
                version += "\n\n*) " + context.getString(R.string.donation_thanks);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Preference versionPreference = screen.findPreference(VERSION_KEY);
        requireNonNull(versionPreference).setSummary(version);
    }
}
