package com.example.medicationapp.domain;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlanValidatorTest {
    @Test public void whitespaceAndMissingUnitRejected() {
        MedicationPlan p = NextOccurrenceTest.plan(); p.name = "  "; p.unit = " ";
        assertEquals(PlanValidator.Error.REQUIRED, PlanValidator.validate(p).get("name"));
        assertEquals(PlanValidator.Error.REQUIRED, PlanValidator.validate(p).get("unit"));
    }
    @Test public void quantitiesMustBePositiveFiniteNumbers() {
        for (String value : new String[]{"0", "-1", "NaN", "Infinity", "", "texto"}) {
            MedicationPlan p = NextOccurrenceTest.plan(); p.quantity = value;
            assertEquals(value, PlanValidator.Error.POSITIVE, PlanValidator.validate(p).get("quantity"));
        }
    }
    @Test public void decimalCommaAndTrimmingNormalized() {
        MedicationPlan p = NextOccurrenceTest.plan(); p.name = " Ejemplo "; p.unit = " unidad "; p.quantity = "0,50";
        assertTrue(PlanValidator.validate(p).isEmpty()); PlanValidator.normalize(p);
        assertEquals("0.5", p.quantity); assertEquals("Ejemplo", p.name); assertEquals("unidad", p.unit);
    }
    @Test public void missingInvalidAndDuplicateTimesRejected() {
        MedicationPlan p = NextOccurrenceTest.plan(); p.times.clear();
        assertEquals(PlanValidator.Error.TIMES, PlanValidator.validate(p).get("times"));
        p.times.add("25:00"); assertEquals(PlanValidator.Error.TIMES, PlanValidator.validate(p).get("times"));
        p.times.clear(); p.times.add("08:00"); p.times.add("08:00:00");
        assertEquals(PlanValidator.Error.DUPLICATE, PlanValidator.validate(p).get("times"));
    }
    @Test public void weeklyRequiresIsoDays() {
        MedicationPlan p = NextOccurrenceTest.plan(); p.recurrence = MedicationPlan.Recurrence.WEEKLY;
        assertEquals(PlanValidator.Error.DAYS, PlanValidator.validate(p).get("weekdays"));
        p.weekdays.add(8); assertEquals(PlanValidator.Error.DAYS, PlanValidator.validate(p).get("weekdays"));
    }
    @Test public void intervalRequiresPositiveInteger() {
        MedicationPlan p = NextOccurrenceTest.plan(); p.recurrence = MedicationPlan.Recurrence.EVERY_N_DAYS;
        for (String interval : new String[]{"0", "-1", "1.5", "", "2147483648"}) {
            p.intervalDays = interval;
            assertEquals(PlanValidator.Error.INTERVAL, PlanValidator.validate(p).get("intervalDays"));
        }
        p.intervalDays = " 3 "; p.times.set(0, "08:00:00");
        assertTrue(PlanValidator.validate(p).isEmpty()); PlanValidator.normalize(p);
        assertEquals("3", p.intervalDays); assertEquals("08:00", p.times.get(0));
    }
    @Test public void irrelevantRecurrenceFieldsIgnoredAndCleared() {
        MedicationPlan p = NextOccurrenceTest.plan(); p.weekdays.add(8); p.intervalDays = "invalid";
        assertTrue(PlanValidator.validate(p).isEmpty()); PlanValidator.normalize(p);
        assertTrue(p.weekdays.isEmpty()); assertEquals("1", p.intervalDays);
    }
    @Test public void invalidDatesAndReversedRangeRejected() {
        MedicationPlan p = NextOccurrenceTest.plan(); p.startDate = "2026-02-30";
        assertEquals(PlanValidator.Error.DATE, PlanValidator.validate(p).get("startDate"));
        p.startDate = "2026-02-01"; p.endDate = "2026-01-31";
        assertEquals(PlanValidator.Error.END_BEFORE_START, PlanValidator.validate(p).get("endDate"));
        p.endDate = "2026-02-01"; assertTrue(PlanValidator.validate(p).isEmpty());
    }
}
