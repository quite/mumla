package se.lublin.mumla.preference;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;

import se.lublin.mumla.R;

public abstract class MumlaPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String fragment = preference.getFragment();
        if (fragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString("fragmentClassName", fragment);
            bundle.putCharSequence("title", preference.getTitle());
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

    @Override
    public void onResume() {
        super.onResume();

        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        Bundle arguments = getArguments();
        CharSequence title = (arguments != null) ? arguments.getCharSequence("title") : null;
        if (title == null) {
            actionBar.setTitle(R.string.action_settings);
            return;
        }
        actionBar.setTitle(title);
    }
}
