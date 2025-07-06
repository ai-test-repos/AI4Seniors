package com.ai.ai4seniors.data;

public record MedicationInfo(
        String name,
        String strength,
        String dosageInstruction,
        int frequencyHours,
        int durationDays,
        int totalDoses,
        String prescriber,
        String rxNumber,
        String quantity,
        String pharmacy,
        String pharmacyAddress,
        String pharmacyPhone,
        String dateFilled,
        String discardAfter,
        String patientName
) {}
