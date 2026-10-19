package com.example.medicationapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.medicationapp.data.MedicationDatabase;
import com.example.medicationapp.domain.MedicationPlan;
import com.example.medicationapp.domain.NextOccurrence;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private boolean inactive;
    private int generation;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() { load(); handler.postDelayed(this, 30000); }
    };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_main);
        UiSupport.insets(this, R.id.main_root);
        inactive = state != null && state.getBoolean("inactive");
        findViewById(R.id.add).setOnClickListener(v -> startActivity(new Intent(this, EditorActivity.class)));
        findViewById(R.id.filter).setOnClickListener(v -> { inactive = !inactive; load(); });
        findViewById(R.id.retry).setOnClickListener(v -> load());
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); generation++; super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putBoolean("inactive", inactive); super.onSaveInstanceState(out); }

    private void load() {
        int token = ++generation;
        ((Button) findViewById(R.id.filter)).setText(inactive ? R.string.show_active : R.string.show_inactive);
        ((TextView) findViewById(R.id.status)).setText(R.string.loading);
        findViewById(R.id.retry).setVisibility(View.GONE);
        boolean activeFilter = !inactive;
        MedicationDatabase.IO.execute(() -> {
            try {
                List<MedicationPlan> plans = new ArrayList<>();
                for (var complete : MedicationDatabase.get(this).plans().list(activeFilter)) plans.add(complete.toPlan());
                runOnUiThread(() -> { if (!isDestroyed() && token == generation) render(plans); });
            } catch (RuntimeException e) {
                runOnUiThread(() -> { if (!isDestroyed() && token == generation) {
                    ((TextView) findViewById(R.id.status)).setText(R.string.load_error);
                    findViewById(R.id.retry).setVisibility(View.VISIBLE);
                } });
            }
        });
    }
    private void render(List<MedicationPlan> plans) {
        LinearLayout list = findViewById(R.id.list); list.removeAllViews();
        ((TextView) findViewById(R.id.status)).setText(plans.isEmpty()
                ? getString(inactive ? R.string.empty_inactive : R.string.empty_active) : "");
        Instant now = Instant.now(); ZoneId zone = ZoneId.systemDefault();
        String[] days = getResources().getStringArray(R.array.weekdays);
        for (MedicationPlan p : plans) {
            View row = getLayoutInflater().inflate(R.layout.medication_row, list, false);
            UiSupport.text(row, R.id.row_name, p.name + (p.strength.isEmpty() ? "" : " · " + p.strength));
            UiSupport.text(row, R.id.row_dose, getString(R.string.dose_summary, p.quantity, p.unit));
            String recurrence = getString(R.string.daily);
            if (p.recurrence == MedicationPlan.Recurrence.WEEKLY) {
                List<String> names = new ArrayList<>(); for (int day : p.weekdays) names.add(days[day - 1]);
                recurrence = String.join(", ", names);
            } else if (p.recurrence == MedicationPlan.Recurrence.EVERY_N_DAYS)
                recurrence = getString(R.string.interval_summary, p.intervalDays, p.startDate);
            List<String> times = new ArrayList<>(); for (String t : p.times) times.add(UiSupport.time(t));
            UiSupport.text(row, R.id.row_schedule, getString(R.string.schedule_summary, recurrence, String.join(", ", times)));
            var next = NextOccurrence.next(p, now, zone);
            UiSupport.text(row, R.id.row_next, !p.active ? getString(R.string.inactive_no_next)
                    : next.map(i -> getString(R.string.next_dose, i.atZone(zone).format(
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))))
                            .orElse(getString(R.string.no_next)));
            row.findViewById(R.id.row_edit).setOnClickListener(v -> startActivity(
                    new Intent(this, EditorActivity.class).putExtra("id", p.id)));
            Button toggle = row.findViewById(R.id.row_active);
            toggle.setText(p.active ? R.string.deactivate : R.string.reactivate);
            toggle.setOnClickListener(v -> {
                toggle.setEnabled(false);
                MedicationDatabase.IO.execute(() -> {
                    try {
                        MedicationDatabase.get(this).plans().setActive(p.id, !p.active, System.currentTimeMillis());
                        runOnUiThread(() -> { if (!isDestroyed()) load(); });
                    } catch (RuntimeException e) { runOnUiThread(() -> {
                        if (!isDestroyed()) { toggle.setEnabled(true); ((TextView) findViewById(R.id.status)).setText(R.string.save_error); }
                    }); }
                });
            });
            list.addView(row);
        }
    }
}
