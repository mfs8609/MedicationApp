package com.example.medicationapp;

import android.app.Activity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

final class UiSupport {
    private UiSupport() { }
    static void insets(Activity activity, int rootId) {
        View root = activity.findViewById(rootId);
        int left = root.getPaddingLeft(), top = root.getPaddingTop();
        int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets system = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                l = system.left; t = system.top; r = system.right; b = system.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft(); t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight(); b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left + l, top + t, right + r, bottom + b);
            return insets;
        });
        root.requestApplyInsets();
    }
    static void text(View parent, int id, CharSequence value) {
        ((TextView) parent.findViewById(id)).setText(value);
    }
    static String time(String iso) {
        return LocalTime.parse(iso).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }
}
