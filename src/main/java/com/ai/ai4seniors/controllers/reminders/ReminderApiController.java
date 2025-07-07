package com.ai.ai4seniors.controllers.reminders;

import com.ai.ai4seniors.data.MedicationInfo;
import com.ai.ai4seniors.data.ReminderInfo;
import com.ai.ai4seniors.services.CalendarService;
import com.ai.ai4seniors.services.DriveService;
import com.ai.ai4seniors.services.ParseService;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ReminderApiController {

    @Autowired
    DriveService driveService;

    @Autowired
    ParseService parseService;

    @Autowired
    CalendarService calendarService;


    @GetMapping("/setReminders")
    public ResponseEntity<String> setReminders() {
        try {
            List<MedicationInfo> meds = driveService.extractCSV(); // implement CSV parsing to MedicationInfo list
            createMedicationReminders(meds);
            System.out.println(meds.toString());
            return ResponseEntity.ok("Reminders set for " + meds.size() + " medications.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/clearReminders")
    public ResponseEntity<String> clearRemindersByRx() {
        try {
            List<MedicationInfo> reminders = driveService.extractCSV();
            Calendar calendar = calendarService.getCalendarService();
            int deleted = 0;

            for (MedicationInfo info : reminders) {
                // Fetch events with matching rxNumber in extendedProperties
                Events events = calendar.events().list("primary")
                        .setPrivateExtendedProperty(Collections.singletonList("rxNumber=" + info.rxNumber()))
                        .setMaxResults(250) // Adjust as needed
                        .setSingleEvents(true)
                        .execute();

                for (Event event : events.getItems()) {
                    calendar.events().delete("primary", event.getId()).execute();
                    deleted++;
                }
            }

            return ResponseEntity.ok("Deleted " + deleted + " medication reminders by RxNumber.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to clear reminders: " + e.getMessage());
        }
    }



    public void createMedicationReminders(List<MedicationInfo> meds) throws Exception {
        Calendar calendar = calendarService.getCalendarService();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        for (MedicationInfo info : meds) {
            LocalDateTime startDateTime = LocalDateTime.now().plusMinutes(5); // or parse from info.dateFilled()

            for (int i = 0; i < info.totalDoses(); i++) {
                LocalDateTime occurrence = startDateTime.plusHours((long) info.frequencyHours() * i);
                ZonedDateTime zoned = occurrence.atZone(ZoneId.systemDefault());
                String formatted = formatter.format(zoned);
                DateTime googleDateTime = new DateTime(formatted);
                DateTime endDateTime = new DateTime(
                        formatter.format(zoned.plusMinutes(15)) // Add 15 min duration
                );

                Event event = new Event()
                        .setSummary("Take: " + info.name() + " " + info.strength())
                        .setDescription("Instruction: " + info.dosageInstruction() + "\nRx#" + info.rxNumber())
                        .setStart(new EventDateTime().setDateTime(googleDateTime))
                        .setEnd(new EventDateTime().setDateTime(endDateTime))
                        .setExtendedProperties(new Event.ExtendedProperties().setPrivate(
                                Map.of("rxNumber", info.rxNumber())
                        ));

                calendar.events().insert("primary", event).execute();
            }
        }
    }


    @GetMapping("/getReminders")
    public ResponseEntity<List<ReminderInfo>> getReminders() {
        try {
            List<ReminderInfo> reminders = fetchMedicationReminders();
            return ResponseEntity.ok(reminders);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    public List<ReminderInfo> fetchMedicationReminders() throws Exception {
        Events events = calendarService.getCalendarService().events().list("primary")
                .setTimeMin(new DateTime(System.currentTimeMillis()))
                .setSingleEvents(true)
                .setMaxResults(100)
                .execute();

        return events.getItems().stream()
                .filter(e -> e.getDescription() != null )
                .map(e -> new ReminderInfo(
                        e.getSummary(),
                        e.getDescription(),
                        e.getStart().getDateTime() != null ? e.getStart().getDateTime().toString() : "",
                        e.getEnd().getDateTime() != null ? e.getEnd().getDateTime().toString() : ""
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/shareOnce")// temporary – remove after running once
    public void shareServiceAccountCalendar() throws Exception {
        Calendar calendar = calendarService.getCalendarService(); // your injected Calendar client

        AclRule rule = new AclRule()
                .setRole("owner")
                .setScope(new AclRule.Scope().setType("user").setValue("devdoe00@gmail.com"));

        AclRule insertedRule = calendar.acl().insert("primary", rule).execute();
        System.out.println("created rule id " + insertedRule.getId());
    }

    @GetMapping("/revokeShare")// temporary – remove after running once
    public void unlinkServiceAccountCalendar() throws Exception {
        String aclRuleId = "";
        Calendar calendar = calendarService.getCalendarService(); // your injected Calendar client
       // calendar.acl().delete("primary", aclRuleId).execute();

        String aclId = "user:devdoe00@gmail.com";
        calendar.acl().delete("primary", aclId).execute();
        System.out.println("Unlinked calendar from: " + aclId);
        System.out.println("Access removed for: " + aclRuleId);
    }

}
