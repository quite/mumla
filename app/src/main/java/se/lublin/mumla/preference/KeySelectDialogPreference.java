package se.lublin.mumla.preference;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.DialogPreference;

public class KeySelectDialogPreference extends DialogPreference {
    public KeySelectDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        //  TODO The dialog logic for this preference will need a similar
        // PreferenceDialogFragmentCompat implementation if you want to restore its functionality.
        // For now, this makes it not crash.z
    }
}
