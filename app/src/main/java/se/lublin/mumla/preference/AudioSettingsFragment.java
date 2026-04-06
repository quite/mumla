package se.lublin.mumla.preference;

import static java.util.Objects.requireNonNull;
import static se.lublin.mumla.Settings.DEFAULT_ECHO_CANCELLATION_METHOD;
import static se.lublin.mumla.Settings.PREF_ECHO_CANCELLATION_METHOD;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import se.lublin.mumla.R;
import se.lublin.mumla.Settings;

public class AudioSettingsFragment extends MumlaPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_audio, rootKey);

        ListPreference inputPreference = getPreferenceScreen().findPreference(Settings.PREF_INPUT_METHOD);
        requireNonNull(inputPreference).setOnPreferenceChangeListener((preference, newValue) -> {
            updateAudioDependents(getPreferenceScreen(), (String) newValue);
            return true;
        });

        // Scan each bitrate and determine if the device supports it
        ListPreference inputQualityPreference = getPreferenceScreen().findPreference(Settings.PREF_INPUT_RATE);
        String[] bitrateNames = new String[requireNonNull(inputQualityPreference).getEntryValues().length];
        for (int x = 0; x < bitrateNames.length; x++) {
            int bitrate = Integer.parseInt(inputQualityPreference.getEntryValues()[x].toString());
            boolean supported = AudioRecord.getMinBufferSize(bitrate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) > 0;
            bitrateNames[x] = bitrate + "Hz" + (supported ? "" : " (unsupported)");
        }
        inputQualityPreference.setEntries(bitrateNames);

        ListPreference echoCancellationPref = findPreference(PREF_ECHO_CANCELLATION_METHOD);
        if (echoCancellationPref != null) {
            if (!AcousticEchoCanceler.isAvailable()) {
                // android.media.audiofx.AcousticEchoCanceler is unavailable
                removeListEntry(echoCancellationPref, "system");
            }
            // Fallback to default ("none") if unset, or current value is no longer available
            String current = echoCancellationPref.getValue();
            if (current == null || !Arrays.asList(echoCancellationPref.getEntryValues()).contains(current)) {
                echoCancellationPref.setValue(DEFAULT_ECHO_CANCELLATION_METHOD);
            }
        }

        updateAudioDependents(getPreferenceScreen(), inputPreference.getValue());
    }

    private void removeListEntry(ListPreference pref, String valueToRemove) {
        List<CharSequence> entries = new ArrayList<>(Arrays.asList(pref.getEntries()));
        List<CharSequence> values = new ArrayList<>(Arrays.asList(pref.getEntryValues()));
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).toString().equals(valueToRemove)) {
                entries.remove(i);
                values.remove(i);
            }
        }
        pref.setEntries(entries.toArray(new CharSequence[0]));
        pref.setEntryValues(values.toArray(new CharSequence[0]));
    }

    private static void updateAudioDependents(PreferenceScreen screen, String inputMethod) {
        PreferenceCategory pttCategory = screen.findPreference("ptt_settings");
        PreferenceCategory vadCategory = screen.findPreference("vad_settings");
        requireNonNull(pttCategory).setEnabled(Settings.ARRAY_INPUT_METHOD_PTT.equals(inputMethod));
        requireNonNull(vadCategory).setEnabled(Settings.ARRAY_INPUT_METHOD_VOICE.equals(inputMethod));
    }
}
