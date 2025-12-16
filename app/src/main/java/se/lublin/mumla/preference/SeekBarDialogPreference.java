package se.lublin.mumla.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import se.lublin.mumla.R;

public class SeekBarDialogPreference extends DialogPreference {

    public final int mMax;
    public final int mMin;
    public final int mMultiplier;
    public final String mSuffix;
    public final int mDefaultValue;

    public SeekBarDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        try (TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.SeekBarDialogPreference, 0, 0)) {
            mMax = a.getInt(R.styleable.SeekBarDialogPreference_max, 100);
            mMin = a.getInt(R.styleable.SeekBarDialogPreference_min, 0);
            mMultiplier = a.getInt(R.styleable.SeekBarDialogPreference_multiplier, 1);
            mSuffix = a.getString(R.styleable.SeekBarDialogPreference_android_text);
            mDefaultValue = a.getInt(R.styleable.SeekBarDialogPreference_android_defaultValue, 0);
        }

        setDialogLayoutResource(R.layout.dialog_seekbar_preference);
    }
}
