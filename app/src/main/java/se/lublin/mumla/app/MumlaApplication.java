package se.lublin.mumla.app;

import static se.lublin.mumla.Settings.ARRAY_THEME_DARK;
import static se.lublin.mumla.Settings.ARRAY_THEME_LIGHT;
import static se.lublin.mumla.Settings.ARRAY_THEME_SYSTEM;
import static se.lublin.mumla.Settings.PREF_THEME;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class MumlaApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        applyTheme(preferences);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (PREF_THEME.equals(key)) {
            applyTheme(preferences);
        }
    }

    private void applyTheme(SharedPreferences preferences) {
        boolean legacyValue = false;
        switch (preferences.getString(PREF_THEME, ARRAY_THEME_SYSTEM)) {
            case ARRAY_THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case ARRAY_THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case ARRAY_THEME_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                legacyValue = true;
                break;
        }
        if (legacyValue) {
            preferences.edit().putString(PREF_THEME, ARRAY_THEME_SYSTEM).apply();
        }
    }

}
