package com.ai.ai4seniors.services;

import com.ai.ai4seniors.data.MedicationInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DriveService {

    @Value("${drive.credentials.path}")
    private String apiKeyPath;

    @Value("${drive.app.name}")
    private String applicationName;

    @Value("${drive.folder.id}")
    private String driveFolderId;

    @Value("${drive.file.name}")
    private String fileNameInDrive;

    public Drive getDriveService() throws IOException {
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(apiKeyPath))
                .createScoped(Collections.singleton(DriveScopes.DRIVE));

        return new Drive.Builder(
                credential.getTransport(),
                credential.getJsonFactory(),
                credential)
                .setApplicationName(applicationName)
                .build();
    }

    public java.io.File downloadCsv() throws IOException {
        Drive driveService = getDriveService();
        String query = String.format("name='%s' and '%s' in parents and trashed=false", fileNameInDrive, driveFolderId);
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles().isEmpty()) return new java.io.File("/tmp/" + fileNameInDrive); // fallback for new

        String fileId = result.getFiles().get(0).getId();
        java.io.File outFile = new java.io.File("/tmp/" + fileNameInDrive);
        try (FileOutputStream output = new FileOutputStream(outFile)) {
            driveService.files().get(fileId).executeMediaAndDownloadTo(output);
        }
        return outFile;
    }

    public List<MedicationInfo> extractCSV() throws IOException {
        List<MedicationInfo> reminders = null;
        try {
            java.io.File csvFile = downloadCsv();
            reminders = new ArrayList<>();
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(csvFile));
            for (CSVRecord record : parser) {
                reminders.add(new MedicationInfo(
                        record.get("Name"),
                        record.get("Strength"),
                        record.get("Dosage Instruction"),
                        Integer.parseInt(record.get("Frequency Hours")),
                        Integer.parseInt(record.get("Duration Days")),
                        Integer.parseInt(record.get("Total Doses")),
                        record.get("Prescriber"),
                        record.get("Rx Number"),
                        record.get("Quantity"),
                        record.get("Pharmacy"),
                        record.get("Pharmacy Address"),
                        record.get("Pharmacy Phone"),
                        record.get("Date Filled"),
                        record.get("Discard After"),
                        record.get("Patient Name")
                ));
            }
            return reminders;
        }
     catch (Exception e) {
            System.out.println(e.getMessage());
            return reminders;
        }
    }

    public String unquote(String val) {
        return val.replaceAll("^\"|\"$", "").trim();
    }

    /**
     * Upload or update the file in a specific folder and delete local copy after upload
     */
    public String uploadOrUpdateCsv(java.io.File csvFile) throws IOException {
        Drive driveService = getDriveService();
        String query = String.format("name='%s' and '%s' in parents and trashed=false", fileNameInDrive, driveFolderId);
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        FileContent mediaContent = new FileContent("text/csv", csvFile);
        File uploadedFile;

        if (!result.getFiles().isEmpty()) {
            String fileId = result.getFiles().get(0).getId();
            File updateMetadata = new File();
            updateMetadata.setName(fileNameInDrive);
            uploadedFile = driveService.files().update(fileId, updateMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();
        } else {
            File createMetadata = new File();
            createMetadata.setName(fileNameInDrive);
            createMetadata.setMimeType("text/csv");
            createMetadata.setParents(Collections.singletonList(driveFolderId));

            uploadedFile = driveService.files().create(createMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();

            Permission permission = new Permission()
                    .setType("anyone")
                    .setRole("reader");
            driveService.permissions().create(uploadedFile.getId(), permission).execute();
        }
        if (csvFile.exists()) {
            csvFile.delete();
        }

        return uploadedFile.getWebViewLink();
    }
}


