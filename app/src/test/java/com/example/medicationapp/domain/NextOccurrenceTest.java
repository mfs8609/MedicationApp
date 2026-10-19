package com.example.medicationapp.domain;

import org.junit.Test;
import java.time.*;
import java.util.Arrays;
import static org.junit.Assert.*;

public class NextOccurrenceTest {
    static MedicationPlan plan() {
        MedicationPlan p = new MedicationPlan(); p.name = "Ejemplo"; p.quantity = "1"; p.unit = "unidad";
        p.startDate = "2026-01-01"; p.times.add("08:00"); return p;
    }
    private String next(MedicationPlan p, String now) {
        return NextOccurrence.next(p, Instant.parse(now), ZoneOffset.UTC).map(Instant::toString).orElse("none");
    }
    @Test public void dailyMultipleTimesAndStrictBoundary() {
        MedicationPlan p = plan(); p.times.add("20:00");
        assertEquals("2026-01-01T20:00:00Z", next(p, "2026-01-01T08:00:00Z"));
        assertEquals("2026-01-02T08:00:00Z", next(p, "2026-01-01T23:59:59Z"));
    }
    @Test public void weeklyAdvancesToFollowingWeek() {
        MedicationPlan p = plan(); p.recurrence = MedicationPlan.Recurrence.WEEKLY;
        p.weekdays.addAll(Arrays.asList(1, 3));
        assertEquals("2026-01-05T08:00:00Z", next(p, "2026-01-01T00:00:00Z"));
        assertEquals("2026-01-07T08:00:00Z", next(p, "2026-01-05T08:00:00Z"));
        assertEquals("2026-01-12T08:00:00Z", next(p, "2026-01-07T08:00:00Z"));
    }
    @Test public void everyNDaysAnchoredAcrossMonth() {
        MedicationPlan p = plan(); p.startDate = "2026-01-30";
        p.recurrence = MedicationPlan.Recurrence.EVERY_N_DAYS; p.intervalDays = "3";
        assertEquals("2026-02-02T08:00:00Z", next(p, "2026-01-31T00:00:00Z"));
        assertEquals("2026-02-05T08:00:00Z", next(p, "2026-02-02T08:00:00Z"));
    }
    @Test public void startAndEndInclusiveButNowExclusive() {
        MedicationPlan p = plan(); p.endDate = p.startDate;
        assertEquals("2026-01-01T08:00:00Z", next(p, "2025-12-31T23:00:00Z"));
        assertEquals("none", next(p, "2026-01-01T08:00:00Z"));
        assertEquals("none", next(p, "2026-02-01T00:00:00Z"));
    }
    @Test public void inactiveHasNoOccurrenceAndReactivationKeepsSchedule() {
        MedicationPlan p = plan(); p.active = false;
        assertEquals("none", next(p, "2026-01-01T00:00:00Z"));
        p.active = true; assertEquals("2026-01-01T08:00:00Z", next(p, "2026-01-01T00:00:00Z"));
    }
    @Test public void weeklyOutsideEndHasNoOccurrence() {
        MedicationPlan p = plan(); p.recurrence = MedicationPlan.Recurrence.WEEKLY;
        p.weekdays.add(1); p.endDate = "2026-01-04";
        assertEquals("none", next(p, "2026-01-01T00:00:00Z"));
    }
    @Test public void changedScheduleChangesNextOccurrence() {
        MedicationPlan p = plan(); p.times.set(0, "10:00");
        assertEquals("2026-01-01T10:00:00Z", next(p, "2026-01-01T09:00:00Z"));
    }
    @Test public void intervalUsesCalendarDaysAcrossDst() {
        MedicationPlan p = plan(); p.startDate = "2026-03-07";
        p.recurrence = MedicationPlan.Recurrence.EVERY_N_DAYS; p.intervalDays = "2";
        assertEquals(Instant.parse("2026-03-09T12:00:00Z"), NextOccurrence.next(p,
                Instant.parse("2026-03-07T13:00:00Z"), ZoneId.of("America/New_York")).get());
    }
    @Test public void gapAndOverlapHaveExplicitResolution() {
        MedicationPlan p = plan(); p.startDate = "2026-03-08"; p.times.set(0, "02:30");
        ZoneId zone = ZoneId.of("America/New_York");
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), NextOccurrence.next(p,
                Instant.parse("2026-03-08T05:00:00Z"), zone).get());
        p.startDate = "2026-11-01"; p.times.set(0, "01:30");
        assertEquals(Instant.parse("2026-11-01T06:30:00Z"), NextOccurrence.next(p,
                Instant.parse("2026-11-01T05:30:00Z"), zone).get());
    }
}
