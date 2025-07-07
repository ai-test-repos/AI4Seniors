package com.ai.ai4seniors.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import com.google.api.services.calendar.Calendar;
import java.util.Collections;

@Service
public class CalendarService {

    @Value("${drive.credentials.path}")
    private String apiKeyPath;

    public Calendar getCalendarService() throws Exception {
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(apiKeyPath))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/calendar"));

        return new com.google.api.services.calendar.Calendar.Builder(
                credential.getTransport(),
                credential.getJsonFactory(),
                credential)
                .setApplicationName("Medication Reminder")
                .build();
    }
}
