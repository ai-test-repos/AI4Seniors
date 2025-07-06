package com.ai.ai4seniors.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

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


