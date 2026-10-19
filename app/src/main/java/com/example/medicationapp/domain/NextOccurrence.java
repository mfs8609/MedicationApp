package com.example.medicationapp.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class NextOccurrence {
    private NextOccurrence() { }

    /** Gaps move forward by the DST gap; overlaps consider both offsets. No clock access. */
    public static Optional<Instant> next(MedicationPlan p, Instant reference, ZoneId zone) {
        if (!p.active || !PlanValidator.validate(p).isEmpty()) return Optional.empty();
        LocalDate start = LocalDate.parse(p.startDate);
        LocalDate end = p.endDate.isEmpty() ? LocalDate.MAX : LocalDate.parse(p.endDate);
        LocalDate date = reference.atZone(zone).toLocalDate();
        if (date.isBefore(start)) date = start;
        int interval = p.recurrence == MedicationPlan.Recurrence.EVERY_N_DAYS
                ? Integer.parseInt(p.intervalDays.trim()) : 1;
        if (p.recurrence == MedicationPlan.Recurrence.EVERY_N_DAYS) {
            long remainder = ChronoUnit.DAYS.between(start, date) % interval;
            if (remainder != 0) date = date.plusDays(interval - remainder);
        }
        // One scheduled date plus its successor suffice; weekly needs at most two weeks.
        int limit = p.recurrence == MedicationPlan.Recurrence.WEEKLY ? 14 : 2;
        for (int attempt = 0; attempt < limit && !date.isAfter(end); attempt++) {
            Instant best = null;
            if (p.recurrence != MedicationPlan.Recurrence.WEEKLY || p.weekdays.contains(date.getDayOfWeek().getValue())) {
                for (String time : p.times) {
                    LocalDateTime local = date.atTime(LocalTime.parse(time));
                    var offsets = zone.getRules().getValidOffsets(local);
                    if (offsets.isEmpty()) {
                        Instant candidate = local.atZone(zone).toInstant();
                        if (candidate.isAfter(reference) && (best == null || candidate.isBefore(best))) best = candidate;
                    } else for (var offset : offsets) {
                        Instant candidate = local.toInstant(offset);
                        if (candidate.isAfter(reference) && (best == null || candidate.isBefore(best))) best = candidate;
                    }
                }
            }
            if (best != null) return Optional.of(best);
            if (date.toEpochDay() > LocalDate.MAX.toEpochDay() - interval) break;
            date = date.plusDays(interval);
        }
        return Optional.empty();
    }
}
