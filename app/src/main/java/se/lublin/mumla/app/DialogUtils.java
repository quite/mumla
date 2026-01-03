package se.lublin.mumla.app;

import static androidx.core.text.HtmlCompat.fromHtml;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import se.lublin.mumla.BuildConfig;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;

public final class DialogUtils {
    // Preventing instantiation
    private DialogUtils() {
    }

    public static boolean maybeShowNewsDialog(Context context) {
        if (Settings.getInstance(context).getNewsShownVersions().contains(BuildConfig.VERSIONTAG)) {
            return false;
        }
        showNewsDialogNow(context, BuildConfig.VERSIONTAG, true);
        return true;
    }

    public static void showNewsDialog(Context context) {
        showNewsDialogNow(context, BuildConfig.VERSIONTAG, false);
    }

    private static void showNewsDialogNow(Context context, String version, boolean markShown) {
        String resourceName = "app_news_items_v" + version.replaceAll("[.-]", "_");
        int resourceId = context.getResources().getIdentifier(resourceName, "array", context.getPackageName());
        if (resourceId == 0) {
            Toast.makeText(context, "No news found for version " + version, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<b>%s %s</b><br/><br/>", context.getString(R.string.version), version));
        String[] newsItems = context.getResources().getStringArray(resourceId);
        for (String line : newsItems) {
            sb.append(String.format("● %s<br/>", line));
        }
        if (markShown) {
            sb.append(String.format("<br/><em>%s</em>", context.getString(R.string.app_news_footer_on_startup)));
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_news, null);
        TextView newsTextView = dialogView.findViewById(R.id.news_text_view);
        newsTextView.setText(fromHtml(sb.toString(), 0));

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.app_news)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (markShown) {
                        Settings.getInstance(context).addNewsShownVersion(version);
                    }
                    dialog.dismiss();
                })
                .show();
    }
}
