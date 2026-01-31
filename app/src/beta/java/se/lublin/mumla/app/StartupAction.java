package se.lublin.mumla.app;

import static se.lublin.mumla.app.DialogUtils.maybeShowNewsDialog;

import android.app.Activity;

import androidx.annotation.NonNull;

public class StartupAction implements IStartupAction {
    @Override
    public void execute(@NonNull Activity activity) {
        maybeShowNewsDialog(activity);
    }
}
