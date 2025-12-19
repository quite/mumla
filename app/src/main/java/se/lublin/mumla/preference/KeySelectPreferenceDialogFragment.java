package se.lublin.mumla.preference;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import se.lublin.mumla.R;

public class KeySelectPreferenceDialogFragment extends PreferenceDialogFragmentCompat implements OnKeyListener {
    private TextView mValueView;
    private int mCurrentValue;

    public static KeySelectPreferenceDialogFragment newInstance(String key) {
        final KeySelectPreferenceDialogFragment fragment = new KeySelectPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setNeutralButton(R.string.reset_key, (dialog, which) -> {
            KeySelectDialogPreference preference = (KeySelectDialogPreference) getPreference();
            mCurrentValue = 0;
            // A NeutralButton causes onDialogClosed to be called with positiveResult==false,
            // so we persist manually here.
            if (preference.callChangeListener(mCurrentValue)) {
                requireNonNull(preference.getSharedPreferences())
                        .edit().putInt(preference.getKey(), mCurrentValue).apply();
            }
        });
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        view.setOnKeyListener(this);
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        mValueView = view.findViewById(R.id.key_select_value_view);
        KeySelectDialogPreference preference = (KeySelectDialogPreference) getPreference();
        mCurrentValue = requireNonNull(preference.getSharedPreferences())
                .getInt(preference.getKey(), 0);
        updateValueView();
    }


    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            mCurrentValue = keyCode;
            updateValueView();
        } else {
            dismiss();
        }
        return true;
    }

    private void updateValueView() {
        if (mCurrentValue == 0) {
            mValueView.setText(R.string.no_ptt_key);
        } else {
            final String stripPrefix = "KEYCODE_";
            String keyName = KeyEvent.keyCodeToString(mCurrentValue);
            if (keyName.startsWith(stripPrefix)) {
                keyName = keyName.substring(stripPrefix.length());
            }
            mValueView.setText(keyName);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            KeySelectDialogPreference preference = (KeySelectDialogPreference) getPreference();
            if (preference.callChangeListener(mCurrentValue)) {
                requireNonNull(preference.getSharedPreferences())
                        .edit().putInt(preference.getKey(), mCurrentValue).apply();
            }
        }
    }
}
