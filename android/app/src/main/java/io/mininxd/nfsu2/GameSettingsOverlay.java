package io.mininxd.nfsu2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Settings shown over the running game (Start + Select, or the touch options menu), so the player never
 * has to go back to the launcher. A row cycles through its values; most changes apply at the next start,
 * which the "Apply and restart" button triggers.
 */
final class GameSettingsOverlay {
    private static Dialog current;

    private GameSettingsOverlay() {}

    static boolean isShowing() { return current != null && current.isShowing(); }

    static void toggle(Activity activity) {
        if (isShowing()) current.dismiss(); else show(activity);
    }

    static void show(Activity activity) {
        if (isShowing()) return;
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0101010);
        bg.setCornerRadius(dp(activity, 14));
        panel.setBackground(bg);

        TextView title = text(activity, activity.getString(R.string.game_settings_title), 18, Color.WHITE);
        title.setPadding(dp(activity, 4), 0, 0, dp(activity, 8));
        panel.addView(title);

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        View first = null;
        for (GameOptions.Option option : GameOptions.ALL) {
            View row = optionRow(activity, option);
            list.addView(row);
            if (first == null) first = row;
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView note = text(activity, activity.getString(R.string.game_settings_note), 12, 0xFFAAAAAA);
        note.setPadding(dp(activity, 4), dp(activity, 8), 0, dp(activity, 6));
        panel.addView(note);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.addView(button(activity, activity.getString(R.string.game_settings_restart), v -> {
            dialog.dismiss();
            activity.startActivity(new Intent(activity, LauncherActivity.class).putExtra("restart_game", true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            activity.finish();
        }));
        buttons.addView(button(activity, activity.getString(R.string.game_settings_launcher), v -> {
            dialog.dismiss();
            activity.startActivity(new Intent(activity, LauncherActivity.class).putExtra("from_game", true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            activity.finish();
        }));
        buttons.addView(button(activity, activity.getString(R.string.game_settings_close), v -> dialog.dismiss()));
        panel.addView(buttons);

        dialog.setContentView(panel);
        boolean[] held = new boolean[2]; // start, select
        dialog.setOnKeyListener((d, code, event) -> {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            if (code == KeyEvent.KEYCODE_BUTTON_START) held[0] = down;
            else if (code == KeyEvent.KEYCODE_BUTTON_SELECT) held[1] = down;
            if (down && ((held[0] && held[1]) || code == KeyEvent.KEYCODE_BUTTON_B) && event.getRepeatCount() == 0) {
                d.dismiss();
                return true;
            }
            return false;
        });
        dialog.setOnDismissListener(d -> current = null);
        current = dialog;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics m = activity.getResources().getDisplayMetrics();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(Math.min(m.widthPixels, Math.round(m.heightPixels * 1.25f)), Math.round(m.heightPixels * 0.9f));
            window.setGravity(Gravity.CENTER);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        if (first != null) first.requestFocus();
    }

    private static View optionRow(Activity activity, GameOptions.Option option) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 12), dp(activity, 9), dp(activity, 12), dp(activity, 9));
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackground(rowBackground(activity));
        TextView name = text(activity, option.title(activity), 15, Color.WHITE);
        TextView value = text(activity, option.label(activity, GameOptions.get(activity, option.key)), 15, 0xFF7FD4FF);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(value);
        row.setOnClickListener(v -> {
            String cur = GameOptions.get(activity, option.key);
            int index = 0;
            for (int i = 0; i < option.values.length; i++) if (option.values[i].equals(cur)) index = i;
            String next = option.values[(index + 1) % option.values.length];
            GameOptions.set(activity, option.key, next);
            value.setText(option.label(activity, next));
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(activity, 4);
        row.setLayoutParams(lp);
        return row;
    }

    private static StateListDrawable rowBackground(Activity activity) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, box(activity, 0xFF2A4A66, 0xFF7FD4FF));
        states.addState(new int[] {android.R.attr.state_pressed}, box(activity, 0xFF2A4A66, 0xFF7FD4FF));
        states.addState(new int[] {}, box(activity, 0xFF1C1C1C, 0xFF303030));
        return states;
    }

    private static GradientDrawable box(Activity activity, int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setStroke(dp(activity, 1), stroke);
        d.setCornerRadius(dp(activity, 8));
        return d;
    }

    private static TextView button(Activity activity, String label, View.OnClickListener click) {
        TextView b = text(activity, label, 14, Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setBackground(rowBackground(activity));
        b.setPadding(dp(activity, 8), dp(activity, 10), dp(activity, 8), dp(activity, 10));
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(activity, 3), 0, dp(activity, 3), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private static TextView text(Activity activity, String s, int sp, int color) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private static int dp(Activity activity, int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
