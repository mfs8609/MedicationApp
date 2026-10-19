package com.example.medicationapp.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import com.example.medicationapp.domain.MedicationPlan;
import com.example.medicationapp.domain.PlanValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {MedicationDatabase.Medication.class, MedicationDatabase.Schedule.class,
        MedicationDatabase.Time.class}, version = 1, exportSchema = true)
public abstract class MedicationDatabase extends RoomDatabase {
    public abstract PlansDao plans();
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static volatile MedicationDatabase instance;

    public static MedicationDatabase get(Context context) {
        if (instance == null) synchronized (MedicationDatabase.class) {
            if (instance == null) instance = Room.databaseBuilder(context.getApplicationContext(),
                    MedicationDatabase.class, "medications.db").build();
        }
        return instance;
    }

    @Entity(tableName = "medications")
    public static class Medication {
        @PrimaryKey(autoGenerate = true) public long id;
        @NonNull public String name = "", strength = "", quantity = "", unit = "", notes = "";
        public boolean active;
        public long createdAt, updatedAt;
    }

    @Entity(tableName = "schedules", foreignKeys = @ForeignKey(entity = Medication.class,
            parentColumns = "id", childColumns = "medicationId", onDelete = ForeignKey.CASCADE),
            indices = @Index(value = "medicationId", unique = true))
    public static class Schedule {
        @PrimaryKey(autoGenerate = true) public long id;
        public long medicationId;
        @NonNull public String recurrence = "DAILY", weekdays = "", startDate = "";
        public String endDate;
        public int intervalDays;
    }

    @Entity(tableName = "schedule_times", foreignKeys = @ForeignKey(entity = Schedule.class,
            parentColumns = "id", childColumns = "scheduleId", onDelete = ForeignKey.CASCADE),
            indices = @Index(value = {"scheduleId", "time"}, unique = true))
    public static class Time {
        @PrimaryKey(autoGenerate = true) public long id;
        public long scheduleId;
        @NonNull public String time = "";
    }

    public static class Scheduled {
        @Embedded public Schedule schedule;
        @Relation(parentColumn = "id", entityColumn = "scheduleId") public List<Time> times;
    }
    public static class Complete {
        @Embedded public Medication medication;
        @Relation(entity = Schedule.class, parentColumn = "id", entityColumn = "medicationId")
        public List<Scheduled> schedules;

        public MedicationPlan toPlan() {
            MedicationPlan p = new MedicationPlan();
            Medication m = medication;
            p.id = m.id; p.name = m.name; p.strength = m.strength; p.quantity = m.quantity;
            p.unit = m.unit; p.notes = m.notes; p.active = m.active;
            p.createdAt = m.createdAt; p.updatedAt = m.updatedAt;
            Schedule s = schedules.get(0).schedule;
            p.recurrence = MedicationPlan.Recurrence.valueOf(s.recurrence);
            p.startDate = s.startDate; p.endDate = s.endDate == null ? "" : s.endDate;
            p.intervalDays = Integer.toString(s.intervalDays);
            if (!s.weekdays.isEmpty()) for (String day : s.weekdays.split(",")) p.weekdays.add(Integer.parseInt(day));
            for (Time t : schedules.get(0).times) p.times.add(t.time);
            p.times.sort(String::compareTo);
            return p;
        }
    }

    @Dao
    public abstract static class PlansDao {
        @Transaction @Query("SELECT * FROM medications WHERE active = :active ORDER BY name COLLATE NOCASE, id")
        public abstract List<Complete> list(boolean active);
        @Transaction @Query("SELECT * FROM medications WHERE id = :id")
        public abstract Complete find(long id);
        @Insert public abstract long insertMedication(Medication value);
        @Update public abstract void updateMedication(Medication value);
        @Insert public abstract long insertSchedule(Schedule value);
        @Insert public abstract void insertTimes(List<Time> values);
        @Query("DELETE FROM schedules WHERE medicationId = :id") protected abstract void removeSchedule(long id);
        @Query("UPDATE medications SET active = :active, updatedAt = :now WHERE id = :id")
        public abstract void setActive(long id, boolean active, long now);

        @Transaction
        public long save(MedicationPlan p, long now) {
            if (!PlanValidator.validate(p).isEmpty()) throw new IllegalArgumentException("Invalid plan");
            PlanValidator.normalize(p);
            Medication m = new Medication();
            m.id = p.id; m.name = p.name; m.strength = p.strength; m.quantity = p.quantity;
            m.unit = p.unit; m.notes = p.notes; m.active = p.active; m.updatedAt = now;
            if (p.id == 0) { m.createdAt = now; m.id = insertMedication(m); }
            else {
                Complete previous = find(p.id);
                if (previous == null) throw new IllegalArgumentException("Missing medication");
                m.createdAt = previous.medication.createdAt;
                updateMedication(m); removeSchedule(m.id);
            }
            Schedule s = new Schedule(); s.medicationId = m.id;
            s.recurrence = p.recurrence.name(); s.startDate = p.startDate;
            s.endDate = p.endDate.isEmpty() ? null : p.endDate;
            s.intervalDays = Integer.parseInt(p.intervalDays);
            List<String> days = new ArrayList<>();
            for (int day : p.weekdays) days.add(Integer.toString(day));
            s.weekdays = String.join(",", days);
            s.id = insertSchedule(s);
            List<Time> values = new ArrayList<>();
            for (String time : p.times) { Time t = new Time(); t.scheduleId = s.id; t.time = time; values.add(t); }
            insertTimes(values);
            return m.id;
        }
    }
}
