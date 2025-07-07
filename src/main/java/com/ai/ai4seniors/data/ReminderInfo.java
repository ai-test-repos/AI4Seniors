package com.ai.ai4seniors.data;

public record ReminderInfo(
    String summary,
    String description,
    String startDateTime,
    String endDateTime
) {}
