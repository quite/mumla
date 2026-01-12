package se.lublin.mumla.app;

import static androidx.core.text.HtmlCompat.fromHtml;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.lublin.mumla.BuildConfig;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;

public final class DialogUtils {
    // Preventing instantiation
    private DialogUtils() {
    }

    private static final Map<String, Integer> NEWS_ITEMS;
    private static final List<String> RELEVANT_VERSIONS;

    static {
        // Using LinkedHasMap because order matters
        NEWS_ITEMS = new LinkedHashMap<>();
        // These *must* be added in ascending semver order (1.2.3, 1.10.1, ...)
        NEWS_ITEMS.put("3.7.0", R.string.app_news_items_v3_7_0);
        RELEVANT_VERSIONS = new ArrayList<>();
        for (String candidate : NEWS_ITEMS.keySet()) {
            // Relevant as long as candidate-version <= build-version; otherwise break
            if (new Version(candidate).compareTo(new Version(BuildConfig.VERSIONTAG)) > 0) {
                break;
            }
            RELEVANT_VERSIONS.add(candidate);
        }
    }

    public static boolean maybeShowNewsDialog(Context context) {
        List<String> toShow = new ArrayList<>();
        Set<String> shown = Settings.getInstance(context).getNewsShownVersions();
        for (String version : RELEVANT_VERSIONS) {
            if (!shown.contains(version)) {
                toShow.add(version);
            }
        }

        if (toShow.isEmpty()) {
            return false;
        }

        showNewsDialogForVersions(context, toShow, true);
        return true;
    }

    public static void showAllNewsDialog(Context context) {
        showNewsDialogForVersions(context, RELEVANT_VERSIONS, false);
    }

    private static void showNewsDialogForVersions(Context context, List<String> versions, boolean markShown) {
        StringBuilder sb = new StringBuilder();
        for (int i = versions.size() - 1; i >= 0; i--) {
            String version = versions.get(i);
            Integer resId = NEWS_ITEMS.get(version);
            if (resId == null) {
                continue;
            }
            sb.append(String.format("<b>%s %s</b><br/>", context.getString(R.string.version), version));
            sb.append(context.getString(resId).replace("\n", "<br/>"));
            sb.append("<br/>");
        }

        if (markShown) {
            sb.append(String.format("<em>%s</em>", context.getString(R.string.app_news_footer_on_startup)));
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
                        Settings.getInstance(context).addNewsShownVersions(versions);
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private static class Version implements Comparable<Version> {
        final int major;
        final int minor;
        final int patch;

        Version(String versionString) {
            // Removing any "-suffix"
            String[] parts = versionString.split("-")[0].split("\\.");
            int maj = 0, min = 0, pat = 0;
            try {
                if (parts.length > 0) maj = Integer.parseInt(parts[0]);
                if (parts.length > 1) min = Integer.parseInt(parts[1]);
                if (parts.length > 2) pat = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                Log.d("DialogUtils", "Failed to parse version string: " + versionString, e);
            }
            this.major = maj;
            this.minor = min;
            this.patch = pat;
        }

        @Override
        public int compareTo(Version other) {
            if (this.major != other.major) {
                return Integer.compare(this.major, other.major);
            }
            if (this.minor != other.minor) {
                return Integer.compare(this.minor, other.minor);
            }
            return Integer.compare(this.patch, other.patch);
        }
    }
}
