package com.example.medicationapp.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Editable values; dates are ISO local dates, times HH:mm, weekdays ISO 1..7. */
public final class MedicationPlan implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Recurrence { DAILY, WEEKLY, EVERY_N_DAYS }
    public long id;
    public String name = "", strength = "", quantity = "", unit = "", notes = "";
    public boolean active = true;
    public long createdAt, updatedAt;
    public Recurrence recurrence = Recurrence.DAILY;
    public String startDate = "", endDate = "", intervalDays = "1";
    public List<Integer> weekdays = new ArrayList<>();
    public List<String> times = new ArrayList<>();
}
