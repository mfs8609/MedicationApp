package com.example.medicationapp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class PlanValidator {
    private PlanValidator() { }
    public enum Error { REQUIRED, POSITIVE, DATE, END_BEFORE_START, TIMES, DUPLICATE, DAYS, INTERVAL }

    public static Map<String, Error> validate(MedicationPlan p) {
        Map<String, Error> errors = new LinkedHashMap<>();
        if (p.name.trim().isEmpty()) errors.put("name", Error.REQUIRED);
        if (p.unit.trim().isEmpty()) errors.put("unit", Error.REQUIRED);
        try {
            if (new BigDecimal(p.quantity.trim().replace(',', '.')).signum() <= 0)
                errors.put("quantity", Error.POSITIVE);
        } catch (NumberFormatException e) { errors.put("quantity", Error.POSITIVE); }
        LocalDate start = null, end = null;
        try { start = LocalDate.parse(p.startDate); }
        catch (RuntimeException e) { errors.put("startDate", Error.DATE); }
        if (!p.endDate.trim().isEmpty()) {
            try { end = LocalDate.parse(p.endDate); }
            catch (RuntimeException e) { errors.put("endDate", Error.DATE); }
        }
        if (start != null && end != null && end.isBefore(start))
            errors.put("endDate", Error.END_BEFORE_START);
        if (p.recurrence == null) errors.put("recurrence", Error.REQUIRED);
        if (p.recurrence == MedicationPlan.Recurrence.WEEKLY &&
                (p.weekdays.isEmpty() || p.weekdays.stream().anyMatch(d -> d == null || d < 1 || d > 7)))
            errors.put("weekdays", Error.DAYS);
        if (p.recurrence == MedicationPlan.Recurrence.EVERY_N_DAYS) {
            try {
                if (Integer.parseInt(p.intervalDays.trim()) < 1) errors.put("intervalDays", Error.INTERVAL);
            } catch (NumberFormatException e) { errors.put("intervalDays", Error.INTERVAL); }
        }
        Set<LocalTime> times = new HashSet<>();
        if (p.times.isEmpty()) errors.put("times", Error.TIMES);
        for (String value : p.times) {
            try {
                LocalTime time = LocalTime.parse(value);
                if (time.getSecond() != 0 || time.getNano() != 0) errors.put("times", Error.TIMES);
                if (!times.add(time)) errors.put("times", Error.DUPLICATE);
            } catch (RuntimeException e) { errors.put("times", Error.TIMES); }
        }
        return errors;
    }

    public static void normalize(MedicationPlan p) {
        p.name = p.name.trim(); p.unit = p.unit.trim(); p.strength = p.strength.trim();
        p.notes = p.notes.trim();
        p.quantity = new BigDecimal(p.quantity.trim().replace(',', '.')).stripTrailingZeros().toPlainString();
        for (int i = 0; i < p.times.size(); i++) p.times.set(i, LocalTime.parse(p.times.get(i))
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT)));
        p.times.sort(String::compareTo);
        if (p.recurrence != MedicationPlan.Recurrence.WEEKLY) p.weekdays.clear();
        if (p.recurrence != MedicationPlan.Recurrence.EVERY_N_DAYS) p.intervalDays = "1";
        else p.intervalDays = Integer.toString(Integer.parseInt(p.intervalDays.trim()));
    }
}
