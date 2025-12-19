package se.lublin.mumla.preference;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;

public abstract class MumlaPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String fragment = preference.getFragment();
        if (fragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString("fragmentClassName", fragment);
            getParentFragmentManager().setFragmentResult("launchFragment", bundle);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof SeekBarDialogPreference) {
            if (getParentFragmentManager().findFragmentByTag("androidx.preference.PreferenceFragment.DIALOG") != null) {
                return;
            }
            final PreferenceDialogFragmentCompat dialogFragment = SeekBarPreferenceDialogFragment.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            return;
        }
        if (preference instanceof KeySelectDialogPreference) {
            if (getParentFragmentManager().findFragmentByTag("androidx.preference.PreferenceFragment.DIALOG") != null) {
                return;
            }
            final PreferenceDialogFragmentCompat dialogFragment = KeySelectPreferenceDialogFragment.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            return;
        }
        super.onDisplayPreferenceDialog(preference);
    }
}
