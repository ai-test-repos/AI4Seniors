package com.ai.ai4seniors.services;

import com.ai.ai4seniors.data.MedicationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CSVService {

    @Value("${medication.csv.path}")
    private String csvFilePath;



    public String writeOrAppendToCSV(List<MedicationInfo> newEntries) throws IOException {
        File csvFile = new File(csvFilePath);
        boolean fileExists = csvFile.exists();

        // Ensure parent directory exists
        File parentDir = csvFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Load existing rows
        Set<String> existingSignatures = new HashSet<>();
        if (fileExists) {
            try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                String line;
                reader.readLine(); // skip header
                while ((line = reader.readLine()) != null) {
                    String[] cols = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1); // CSV-safe split
                    if (cols.length >= 14) {
                        String signature = buildSignature(
                                unquote(cols[0]),
                                unquote(cols[1]),
                                unquote(cols[2]),
                                unquote(cols[7]),
                                unquote(cols[12])
                        );
                        existingSignatures.add(signature);
                    }
                }
            }
        }

        // Prepare to write only new entries
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath, true))) {
            if (!fileExists) {
                writer.println("Name,Strength,Dosage Instruction,Frequency Hours,Duration Days,Total Doses,Prescriber,Rx Number,Quantity,Pharmacy,Pharmacy Address,Pharmacy Phone,Date Filled,Discard After,Patient Name,Extra");
            }

            for (MedicationInfo info : newEntries) {
                String sig = buildSignature(
                        info.name(),
                        info.strength(),
                        info.dosageInstruction(),
                        info.rxNumber(),
                        info.dateFilled()
                );

                if (!existingSignatures.contains(sig)) {
                    writer.printf("\"%s\",\"%s\",\"%s\",%d,%d,%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"\"%n",
                            safe(info.name()),
                            safe(info.strength()),
                            safe(info.dosageInstruction()),
                            info.frequencyHours(),
                            info.durationDays(),
                            info.totalDoses(),
                            safe(info.prescriber()),
                            safe(info.rxNumber()),
                            safe(info.quantity()),
                            safe(info.pharmacy()),
                            safe(info.pharmacyAddress()),
                            safe(info.pharmacyPhone()),
                            safe(info.dateFilled()),
                            safe(info.discardAfter()),
                            safe(info.patientName())
                    );
                }
            }
        }
        return csvFilePath;
    }

    private String buildSignature(String name, String strength, String instruction, String rx, String dateFilled) {
        return (name + "|" + strength + "|" + instruction + "|" + rx + "|" + dateFilled).toLowerCase().trim();
    }

    private String safe(String val) {
        return val == null ? "" : val.replace("\"", "'").trim();
    }

    private String unquote(String val) {
        return val.replaceAll("^\"|\"$", "").trim();
    }

}
