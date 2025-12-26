package se.lublin.mumla.app;

import android.app.Activity;

import static se.lublin.mumla.app.DialogUtils.maybeShowNewsDialog;

public class StartupAction implements IStartupAction {
    @Override
    public void execute(Activity activity) {
        maybeShowNewsDialog(activity);
    }
}
