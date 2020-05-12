/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.lublin.mumla.preference;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import se.lublin.mumla.BuildConfig;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;

/**
 * This entire class is a mess.
 * FIXME. Please.
 */
public class Preferences extends PreferenceActivity {

    public static final String ACTION_PREFS_GENERAL = "se.lublin.mumla.app.PREFS_GENERAL";
    public static final String ACTION_PREFS_AUTHENTICATION = "se.lublin.mumla.app.PREFS_AUTHENTICATION";
    public static final String ACTION_PREFS_AUDIO = "se.lublin.mumla.app.PREFS_AUDIO";
    public static final String ACTION_PREFS_APPEARANCE = "se.lublin.mumla.app.PREFS_APPEARANCE";
    public static final String ACTION_PREFS_ABOUT = "se.lublin.mumla.app.PREFS_ABOUT";

    private static final String USE_TOR_KEY = "useTor";
    private static final String VERSION_KEY = "version";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(Settings.getInstance(this).getTheme());
        super.onCreate(savedInstanceState);

        // Legacy preference section handling
        String action = getIntent().getAction();
        if (action != null) {
            if (ACTION_PREFS_GENERAL.equals(action)) {
                addPreferencesFromResource(R.xml.settings_general);
                configureOrbotPreferences(getPreferenceScreen());
            } else if (ACTION_PREFS_AUTHENTICATION.equals(action)) {
                addPreferencesFromResource(R.xml.settings_authentication);
            } else if (ACTION_PREFS_AUDIO.equals(action)) {
                addPreferencesFromResource(R.xml.settings_audio);
                configureAudioPreferences(getPreferenceScreen());
            } else if (ACTION_PREFS_APPEARANCE.equals(action)) {
                addPreferencesFromResource(R.xml.settings_appearance);
            } else if (ACTION_PREFS_ABOUT.equals(action)) {
                addPreferencesFromResource(R.xml.settings_about);
                configureAboutPreferences(this, getPreferenceScreen());
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return MumlaPreferenceFragment.class.getName().equals(fragmentName);
    }

    private static void configureOrbotPreferences(PreferenceScreen screen) {
        Preference useOrbotPreference = screen.findPreference(USE_TOR_KEY);
        useOrbotPreference.setEnabled(OrbotHelper.isOrbotInstalled(screen.getContext()));
    }

    private static void configureAudioPreferences(final PreferenceScreen screen) {
        ListPreference inputPreference = (ListPreference) screen.findPreference(Settings.PREF_INPUT_METHOD);
        inputPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateAudioDependents(screen, (String) newValue);
                return true;
            }
        });

        // Scan each bitrate and determine if the device supports it
        ListPreference inputQualityPreference = (ListPreference) screen.findPreference(Settings.PREF_INPUT_RATE);
        String[] bitrateNames = new String[inputQualityPreference.getEntryValues().length];
        for(int x=0;x<bitrateNames.length;x++) {
            int bitrate = Integer.parseInt(inputQualityPreference.getEntryValues()[x].toString());
            boolean supported = AudioRecord.getMinBufferSize(bitrate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) > 0;
            bitrateNames[x] = bitrate+"Hz" + (supported ? "" : " (unsupported)");
        }
        inputQualityPreference.setEntries(bitrateNames);

        updateAudioDependents(screen, inputPreference.getValue());
    }

    private static void updateAudioDependents(PreferenceScreen screen, String inputMethod) {
        PreferenceCategory pttCategory = (PreferenceCategory) screen.findPreference("ptt_settings");
        PreferenceCategory vadCategory = (PreferenceCategory) screen.findPreference("vad_settings");
        pttCategory.setEnabled(Settings.ARRAY_INPUT_METHOD_PTT.equals(inputMethod));
        vadCategory.setEnabled(Settings.ARRAY_INPUT_METHOD_VOICE.equals(inputMethod));
    }

    private static void configureAboutPreferences(Context context, PreferenceScreen screen) {
        String version = "Unknown";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = info.versionName;
            if (BuildConfig.FLAVOR.equals("beta")) {
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
        versionPreference.setSummary(version);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MumlaPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String section = getArguments().getString("settings");
            if ("general".equals(section)) {
                addPreferencesFromResource(R.xml.settings_general);
                configureOrbotPreferences(getPreferenceScreen());
            } else if ("authentication".equals(section)) {
                addPreferencesFromResource(R.xml.settings_authentication);
            } else if ("audio".equals(section)) {
                addPreferencesFromResource(R.xml.settings_audio);
                configureAudioPreferences(getPreferenceScreen());
            } else if ("appearance".equals(section)) {
                addPreferencesFromResource(R.xml.settings_appearance);
            } else if ("about".equals(section)) {
                addPreferencesFromResource(R.xml.settings_about);
                configureAboutPreferences(getPreferenceScreen().getContext(), getPreferenceScreen());
            }
        }
    }
}
