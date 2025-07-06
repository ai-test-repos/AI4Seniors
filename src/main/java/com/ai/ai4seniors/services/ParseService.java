package com.ai.ai4seniors.services;

import com.ai.ai4seniors.data.MedicationInfo;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParseService {

    public MedicationInfo parseMedicationText(String text) {
        String[] lines = text.split("\\r?\\n");
        String name = "";
        String strength = "";
        String dosageInstruction = "";
        int frequencyHours = 6;
        int durationDays = 3;
        int totalDoses = 0;
        String prescriber = "";
        String rxNumber = "";
        String quantity = "";
        String pharmacy = "";
        String pharmacyAddress = "";
        String pharmacyPhone = "";
        String dateFilled = "";
        String discardAfter = "";
        String patientName = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Attempt to detect patient name early (first 1–4 lines)
            if (patientName.isEmpty() && i < 4) {
                // Match title-case lines with at least two words (e.g., "John Doe")
                if (line.matches("^[A-Z][a-z]+(\\s+[A-Z][a-z]+)+$")) {
                    String lowerLine = line.toLowerCase();
                    if (!lowerLine.contains("pharmacy") &&
                            !lowerLine.contains("tablet") &&
                            !lowerLine.contains("take") &&
                            !lowerLine.contains("mg") &&
                            !lowerLine.contains("capsule") &&
                            !lowerLine.contains("date") &&
                            !lowerLine.contains("rx") &&
                            !line.equals(line.toUpperCase())) { // avoid ALLCAPS
                        patientName = line;
                    }
                }
            }

            // Medication name (capital lines)
            if (name.isEmpty() && line.matches("^[A-Z][A-Z\\s\\-/]{2,}$")) {
                String cleaned = line.replaceAll("(MFR:.*|TABLET|CAPSULE|\\d+MG|\\d+-\\d+MG|\\d+\\s*MG)", "")
                        .replaceAll("[^A-Z\\s]", "")
                        .trim()
                        .replaceAll("\\s{2,}", " ");
                if (!cleaned.isBlank() && cleaned.length() >= 4) {
                    name = cleaned;
                }
            }

            if (strength.isEmpty() && line.matches("(?i).*(\\d+(-\\d+)?\\s*MG).*")) {
                strength = line.replaceAll("(?i).*?(\\d+(-\\d+)?\\s*MG).*", "$1").trim();
            }

            if (line.toLowerCase().contains("take 1 tablet")) {
                dosageInstruction = line;
            }

            if (line.toLowerCase().contains("every") && line.toLowerCase().contains("hours")) {
                Matcher m = Pattern.compile("(?i)every\\s+(\\d+)\\s*hours?").matcher(line);
                if (m.find()) {
                    frequencyHours = Integer.parseInt(m.group(1));
                }
            }

            if (line.toLowerCase().contains("days")) {
                Matcher m = Pattern.compile("(\\d+)\\s+days").matcher(line);
                if (m.find()) {
                    durationDays = Integer.parseInt(m.group(1));
                }
            }

            if (prescriber.isEmpty() && line.toLowerCase().contains("prscbr")) {
                prescriber = line.replaceAll("(?i).*prscbr[:\\s]+", "").trim();
            }

            if (rxNumber.isEmpty() && line.toLowerCase().contains("rx:")) {
                rxNumber = line.replaceAll("(?i).*rx[:\\s]+", "").trim();
            }

            if (quantity.isEmpty() && line.toLowerCase().contains("qty:")) {
                quantity = line.replaceAll("(?i).*qty[:\\s]+", "").trim();
            }

            if (pharmacy.isEmpty() && line.toLowerCase().contains("pharmacy")) {
                pharmacy = line;
                if (i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (!next.toLowerCase().contains("tel")) {
                        pharmacyAddress = next;
                    }
                }
            }

            if (pharmacyPhone.isEmpty() && line.toLowerCase().contains("tel:")) {
                pharmacyPhone = line.replaceAll("(?i).*tel[:\\s]+", "").trim();
            }

            if (dateFilled.isEmpty() && line.toLowerCase().contains("date filled")) {
                dateFilled = line.replaceAll("(?i).*date filled[:\\s]+", "").trim();
            }

            if (discardAfter.isEmpty() && line.toLowerCase().contains("discard after")) {
                discardAfter = line.replaceAll("(?i).*discard after[:\\s]+", "").trim();
            }
        }

        totalDoses = (durationDays * 24) / frequencyHours;

        return new MedicationInfo(
                name,
                strength,
                dosageInstruction,
                frequencyHours,
                durationDays,
                totalDoses,
                prescriber,
                rxNumber,
                quantity,
                pharmacy,
                pharmacyAddress,
                pharmacyPhone,
                dateFilled,
                discardAfter,
                patientName
        );
    }

}
