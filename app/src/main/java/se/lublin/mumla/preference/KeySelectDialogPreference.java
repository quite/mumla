package se.lublin.mumla.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import se.lublin.mumla.R;

public class KeySelectDialogPreference extends DialogPreference {
    public KeySelectDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dialog_keyselect_preference);
    }
}
