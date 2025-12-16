package se.lublin.mumla.preference;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import se.lublin.mumla.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_settings);
        }

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!getSupportFragmentManager().popBackStackImmediate()) {
                    // Finish activity if nothing was popped from stack
                    finish();
                }
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new RootPreferenceFragment())
                    .commit();
        }

        getSupportFragmentManager().setFragmentResultListener("launchFragment", this, (requestKey, result) -> {
            String fragmentClassName = result.getString("fragmentClassName");
            if (fragmentClassName != null) {
                try {
                    Class<?> fragmentClass = Class.forName(fragmentClassName);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.settings_container, (Fragment) fragmentClass.newInstance())
                            .addToBackStack(null)
                            .commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static class RootPreferenceFragment extends MumlaPreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preference_headers, rootKey);
        }
    }
}
