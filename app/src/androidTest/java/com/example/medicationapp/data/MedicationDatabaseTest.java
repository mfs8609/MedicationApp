package com.example.medicationapp.data;

import android.content.Context;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.medicationapp.domain.MedicationPlan;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MedicationDatabaseTest {
    private Context context;
    private MedicationDatabase db;
    private static final String NAME = "spec002-test.db";
    @Before public void open() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(NAME); db = Room.databaseBuilder(context, MedicationDatabase.class, NAME).build();
    }
    @After public void close() { db.close(); context.deleteDatabase(NAME); }
    private MedicationPlan example() {
        MedicationPlan p = new MedicationPlan(); p.name = "Ejemplo ficticio"; p.quantity = "1"; p.unit = "unidad";
        p.startDate = "2026-01-01"; p.times.add("08:00"); return p;
    }
    @Test public void duplicateNamesPersistAndDatabaseReopens() {
        long first = db.plans().save(example(), 1000), second = db.plans().save(example(), 2000);
        assertNotEquals(first, second); db.close();
        db = Room.databaseBuilder(context, MedicationDatabase.class, NAME).build();
        assertEquals(2, db.plans().list(true).size());
        assertEquals("08:00", db.plans().find(first).toPlan().times.get(0));
    }
    @Test public void editAndTogglePreserveIdentityCreationAndSchedule() {
        long id = db.plans().save(example(), 1000);
        MedicationPlan p = db.plans().find(id).toPlan(); p.name = "Ejemplo editado";
        p.times.set(0, "09:00"); db.plans().save(p, 2000);
        db.plans().setActive(id, false, 3000); assertTrue(db.plans().list(true).isEmpty());
        MedicationPlan inactive = db.plans().list(false).get(0).toPlan();
        assertEquals("09:00", inactive.times.get(0)); assertEquals(1000, inactive.createdAt);
        db.plans().setActive(id, true, 4000);
        MedicationPlan active = db.plans().find(id).toPlan();
        assertTrue(active.active); assertEquals("Ejemplo editado", active.name); assertEquals(4000, active.updatedAt);
    }
    @Test public void failedTimeInsertRollsBackEntireEdit() {
        long id = db.plans().save(example(), 1000);
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER fail_time BEFORE INSERT ON schedule_times " +
                "WHEN NEW.time = '09:00' BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        MedicationPlan p = db.plans().find(id).toPlan(); p.name = "Cambio fallido"; p.times.set(0, "09:00");
        try { db.plans().save(p, 2000); fail("Expected failed insert"); } catch (android.database.SQLException expected) { }
        MedicationPlan stored = db.plans().find(id).toPlan();
        assertEquals("Ejemplo ficticio", stored.name); assertEquals("08:00", stored.times.get(0));
        assertEquals(1000, stored.updatedAt);
    }
    @Test public void invalidPlanDoesNotChangeStoredData() {
        long id = db.plans().save(example(), 1000);
        MedicationPlan p = db.plans().find(id).toPlan(); p.times.add("08:00");
        try { db.plans().save(p, 2000); fail("Expected validation failure"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(1, db.plans().find(id).toPlan().times.size());
    }
    @Test public void schemaEnforcesOneScheduleAndUniqueTimes() {
        long id = db.plans().save(example(), 1000);
        MedicationDatabase.Schedule duplicate = new MedicationDatabase.Schedule(); duplicate.medicationId = id;
        try { db.plans().insertSchedule(duplicate); fail("Expected unique schedule constraint"); }
        catch (android.database.SQLException expected) { }
        MedicationDatabase.Time duplicateTime = new MedicationDatabase.Time();
        duplicateTime.scheduleId = db.plans().find(id).schedules.get(0).schedule.id; duplicateTime.time = "08:00";
        try { db.plans().insertTimes(java.util.Collections.singletonList(duplicateTime)); fail("Expected unique time constraint"); }
        catch (android.database.SQLException expected) { }
    }
    @Test public void foreignKeyRejectsOrphanSchedule() {
        MedicationDatabase.Schedule orphan = new MedicationDatabase.Schedule(); orphan.medicationId = 99999;
        try { db.plans().insertSchedule(orphan); fail("Expected foreign key constraint"); }
        catch (android.database.SQLException expected) { }
    }
}
