package com.example.medicationapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.medicationapp.data.MedicationDatabase;
import com.example.medicationapp.domain.MedicationPlan;
import com.example.medicationapp.domain.PlanValidator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EditorActivity extends Activity {
    private MedicationPlan plan;
    private final List<String> times = new ArrayList<>();
    private final List<CheckBox> days = new ArrayList<>();
    private boolean ready, saving;
    private String original;
    private SaveOperation operation;
    private static final class SaveOperation {
        boolean done, failed;
        Runnable listener;
        void start(android.content.Context context, MedicationPlan draft) {
            MedicationDatabase.IO.execute(() -> {
                boolean error = false;
                try { MedicationDatabase.get(context).plans().save(draft, System.currentTimeMillis()); }
                catch (RuntimeException e) { error = true; }
                final boolean result = error;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    failed = result; done = true; if (listener != null) listener.run();
                });
            });
        }
    }
    private void enableForm(boolean enabled) {
        enableTree(findViewById(R.id.editor_form), enabled);
        findViewById(R.id.cancel).setEnabled(!saving);
    }
    private void enableTree(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) enableTree(group.getChildAt(i), enabled);
        }
    }
    private void observeSave() {
        if (operation == null) return;
        operation.listener = () -> {
            if (!operation.done || isDestroyed()) return;
            saving = false;
            if (!operation.failed) { finish(); return; }
            operation = null; enableForm(true); status(R.string.save_error);
        };
        saving = !operation.done;
        if (saving) enableForm(false);
        operation.listener.run();
    }
    private EditText field(int id) { return findViewById(id); }
    private String value(int id) { return field(id).getText().toString(); }
    private void status(int resource) { ((TextView) findViewById(R.id.editor_status)).setText(resource); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_editor);
        UiSupport.insets(this, R.id.editor_root);
        LinearLayout week = findViewById(R.id.week_checks);
        for (String name : getResources().getStringArray(R.array.weekdays)) {
            CheckBox box = new CheckBox(this); box.setText(name); box.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
            week.addView(box); days.add(box);
        }
        ((Spinner) findViewById(R.id.recurrence)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                findViewById(R.id.week_group).setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                findViewById(R.id.interval_group).setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
        });
        findViewById(R.id.add_time).setOnClickListener(v -> pickTime(-1));
        findViewById(R.id.cancel).setOnClickListener(v -> leave());
        findViewById(R.id.save).setOnClickListener(v -> save());
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::leave);
        }
        if (state != null && state.containsKey("draft")) {
            plan = (MedicationPlan) state.getSerializable("draft");
            original = state.getString("original"); show();
        } else if (getIntent().getLongExtra("id", 0) != 0) {
            enableForm(false); status(R.string.loading);
            MedicationDatabase.IO.execute(() -> {
                try {
                    var found = MedicationDatabase.get(this).plans().find(getIntent().getLongExtra("id", 0));
                    if (found == null) throw new IllegalStateException();
                    MedicationPlan loaded = found.toPlan();
                    runOnUiThread(() -> { if (!isDestroyed()) { plan = loaded; show(); } });
                } catch (RuntimeException e) { runOnUiThread(() -> { if (!isDestroyed()) status(R.string.load_error); }); }
            });
        } else { plan = new MedicationPlan(); plan.startDate = LocalDate.now().toString(); show(); }
        operation = (SaveOperation) getLastNonConfigurationInstance();
        observeSave();
    }
    private void show() {
        field(R.id.name).setText(plan.name); field(R.id.strength).setText(plan.strength);
        field(R.id.quantity).setText(plan.quantity); field(R.id.unit).setText(plan.unit);
        field(R.id.notes).setText(plan.notes); field(R.id.start).setText(plan.startDate);
        field(R.id.end).setText(plan.endDate); field(R.id.interval).setText(plan.intervalDays);
        ((CheckBox) findViewById(R.id.active)).setChecked(plan.active);
        ((Spinner) findViewById(R.id.recurrence)).setSelection(plan.recurrence.ordinal());
        for (int i = 0; i < days.size(); i++) days.get(i).setChecked(plan.weekdays.contains(i + 1));
        times.clear(); times.addAll(plan.times); renderTimes();
        ((TextView) findViewById(R.id.editor_title)).setText(plan.id == 0 ? R.string.new_title : R.string.edit_title);
        ((TextView) findViewById(R.id.editor_status)).setText("");
        ready = true; enableForm(true);
        if (original == null) original = fingerprint(read());
    }
    private MedicationPlan read() {
        MedicationPlan p = new MedicationPlan();
        p.id = plan.id; p.createdAt = plan.createdAt; p.updatedAt = plan.updatedAt;
        p.name = value(R.id.name); p.strength = value(R.id.strength); p.quantity = value(R.id.quantity);
        p.unit = value(R.id.unit); p.notes = value(R.id.notes); p.startDate = value(R.id.start).trim();
        p.endDate = value(R.id.end).trim(); p.intervalDays = value(R.id.interval);
        p.active = ((CheckBox) findViewById(R.id.active)).isChecked();
        p.recurrence = MedicationPlan.Recurrence.values()[((Spinner) findViewById(R.id.recurrence)).getSelectedItemPosition()];
        for (int i = 0; i < days.size(); i++) if (days.get(i).isChecked()) p.weekdays.add(i + 1);
        p.times.addAll(times); return p;
    }
    private String fingerprint(MedicationPlan p) {
        // Length-prefix free text so a separator typed in notes cannot mask a change.
        StringBuilder key = new StringBuilder();
        for (String s : new String[]{p.name,p.strength,p.quantity,p.unit,p.notes,p.startDate,p.endDate,p.intervalDays})
            key.append(s.length()).append(':').append(s);
        return key.append(p.active).append(p.recurrence).append(p.weekdays).append(p.times).toString();
    }
    private void renderTimes() {
        LinearLayout rows = findViewById(R.id.time_rows); rows.removeAllViews();
        for (int i = 0; i < times.size(); i++) {
            final int index = i;
            View row = getLayoutInflater().inflate(R.layout.time_row, rows, false);
            UiSupport.text(row, R.id.time_value, UiSupport.time(times.get(i)));
            row.findViewById(R.id.time_value).setOnClickListener(v -> pickTime(index));
            row.findViewById(R.id.time_remove).setContentDescription(getString(R.string.remove_time, UiSupport.time(times.get(i))));
            row.findViewById(R.id.time_remove).setOnClickListener(v -> { times.remove(index); renderTimes(); });
            rows.addView(row);
        }
    }
    private void pickTime(int index) {
        if (!ready || saving) return;
        LocalTime initial = index < 0 ? LocalTime.of(8, 0) : LocalTime.parse(times.get(index));
        new TimePickerDialog(this, (picker, hour, minute) -> {
            String time = LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT));
            if (times.contains(time) && (index < 0 || !times.get(index).equals(time))) {
                ((TextView) findViewById(R.id.time_error)).setError(getString(R.string.duplicate_error)); return;
            }
            if (index < 0) times.add(time); else times.set(index, time);
            times.sort(String::compareTo); renderTimes();
            ((TextView) findViewById(R.id.time_error)).setError(null);
        }, initial.getHour(), initial.getMinute(), android.text.format.DateFormat.is24HourFormat(this)).show();
    }
    private int message(PlanValidator.Error error) {
        switch (error) {
            case POSITIVE: return R.string.positive_error;
            case DATE: return R.string.date_error;
            case END_BEFORE_START: return R.string.end_error;
            case TIMES: return R.string.times_error;
            case DUPLICATE: return R.string.duplicate_error;
            case DAYS: return R.string.days_error;
            case INTERVAL: return R.string.interval_error;
            default: return R.string.required_error;
        }
    }
    private void save() {
        if (!ready || saving) return;
        MedicationPlan draft = read();
        Map<String, Integer> fields = new java.util.HashMap<>();
        fields.put("name", R.id.name); fields.put("quantity", R.id.quantity); fields.put("unit", R.id.unit);
        fields.put("startDate", R.id.start); fields.put("endDate", R.id.end); fields.put("intervalDays", R.id.interval);
        fields.put("weekdays", R.id.week_error); fields.put("times", R.id.time_error);
        for (int id : fields.values()) ((TextView) findViewById(id)).setError(null);
        var errors = PlanValidator.validate(draft);
        if (!errors.isEmpty()) {
            for (var error : errors.entrySet()) {
                Integer id = fields.get(error.getKey());
                if (id != null) ((TextView) findViewById(id)).setError(getString(message(error.getValue())));
            }
            findViewById(fields.get(errors.keySet().iterator().next())).requestFocus(); return;
        }
        operation = new SaveOperation(); observeSave();
        operation.start(getApplicationContext(), draft);
    }
    private void leave() {
        if (saving) return;
        if (!ready || fingerprint(read()).equals(original)) { finish(); return; }
        new AlertDialog.Builder(this).setTitle(R.string.discard_title).setMessage(R.string.discard_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> finish())
                .setNegativeButton(R.string.continue_editing, null).show();
    }
    @Override public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (android.os.Build.VERSION.SDK_INT < 33 && keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            leave(); return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        if (ready) { out.putSerializable("draft", read()); out.putString("original", original); }
        super.onSaveInstanceState(out);
    }
    @Override public Object onRetainNonConfigurationInstance() { return operation; }
    @Override protected void onDestroy() {
        if (operation != null) operation.listener = null;
        super.onDestroy();
    }
}
