package com.ai.ai4seniors.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

@Service
public class DriveService {

    private final String serviceAccountKeyPath = "/Users/shri/Desktop/WS/env/gcv-key.json";
    private final String driveFolderId = "1CpbgyzQcClrmjtzEyGBLN3It7gu8puqt"; // Set your folder ID
    private final String applicationName = "Medication OCR Uploader";
//    private final String localFilePath = "/Users/shri/Desktop/WS/med-logs/medication_log.csv";
//    private final String fileNameInDrive = "medication_log.csv";

    /**
     * Upload or update the file in a specific folder and delete local copy after upload
     */
    public String uploadOrUpdateCsv(String localFilePath, String fileNameInDrive) throws IOException {
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(serviceAccountKeyPath))
                .createScoped(Collections.singleton(DriveScopes.DRIVE));

        Drive driveService = new Drive.Builder(
                credential.getTransport(),
                credential.getJsonFactory(),
                credential)
                .setApplicationName(applicationName)
                .build();

        // Check if file already exists in the target folder
        String query = String.format("name='%s' and '%s' in parents and trashed=false", fileNameInDrive, driveFolderId);
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        java.io.File localFile = new java.io.File(localFilePath);
        FileContent mediaContent = new FileContent("text/csv", localFile);

        File uploadedFile;

        if (!result.getFiles().isEmpty()) {
            // Step 2: File exists → update (DO NOT set parents)
            String fileId = result.getFiles().get(0).getId();

            File updateMetadata = new File(); // do not include `parents` here
            updateMetadata.setName(fileNameInDrive); // optional but safe

            uploadedFile = driveService.files().update(fileId, updateMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();
        } else {
            // Step 3: File does not exist → create (include parents)
            File createMetadata = new File();
            createMetadata.setName(fileNameInDrive);
            createMetadata.setMimeType("text/csv");
            createMetadata.setParents(Collections.singletonList(driveFolderId));

            uploadedFile = driveService.files().create(createMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();

            // Make publicly readable
            Permission permission = new Permission()
                    .setType("anyone")
                    .setRole("reader");
            driveService.permissions().create(uploadedFile.getId(), permission).execute();
        }

        return uploadedFile.getWebViewLink();
    }
}


