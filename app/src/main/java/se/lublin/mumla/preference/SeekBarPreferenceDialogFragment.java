package se.lublin.mumla.preference;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import se.lublin.mumla.R;

public class SeekBarPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private TextView mValueView;
    private int mMultiplier;
    private int mMin;
    private int mCurrentValue;
    private String mSuffix;

    public static SeekBarPreferenceDialogFragment newInstance(String key) {
        final SeekBarPreferenceDialogFragment fragment = new SeekBarPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        SeekBar seekBar = view.findViewById(R.id.seek_bar);
        mValueView = view.findViewById(R.id.seek_bar_value_view);

        SeekBarDialogPreference preference = (SeekBarDialogPreference) getPreference();

        mMultiplier = preference.mMultiplier;
        mMin = preference.mMin;
        mSuffix = preference.mSuffix;
        // The persisted value is always multiplied, but the default value in the XML is not (nor
        // are the min and max values in the XML). Make this variable contain the correct
        // multiplier value in both cases.
        mCurrentValue = requireNonNull(preference.getSharedPreferences())
                .getInt(preference.getKey(), preference.mDefaultValue * mMultiplier);
        updateValueView();

        seekBar.setMax(preference.mMax - mMin);
        // The persisted value is multiplied, scale it down
        seekBar.setProgress((mCurrentValue / mMultiplier) - mMin);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mCurrentValue = (mMin + progress) * mMultiplier;
                    updateValueView();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateValueView() {
        String t = String.valueOf(mCurrentValue);
        mValueView.setText(mSuffix == null ? t : t.concat(mSuffix));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            SeekBarDialogPreference preference = (SeekBarDialogPreference) getPreference();
            // Persisting the multiplied value, as was always done
            if (preference.callChangeListener(mCurrentValue)) {
                requireNonNull(preference.getSharedPreferences())
                        .edit().putInt(preference.getKey(), mCurrentValue).apply();
            }
        }
    }
}
