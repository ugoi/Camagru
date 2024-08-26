package com.camagru.services;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MediaService {

    private static final String MEDIA_DIRECTORY = "uploads/media/";

    public static byte[] getMedia(String id) throws IOException, IllegalArgumentException {
        // Find out all log files
        String fileName = getMediaFileName(id);

        // Get video file from disk
        return Files.readAllBytes(Paths.get(MEDIA_DIRECTORY + fileName));
    }

    public static void hasPermission(String userId, String fileId) throws IllegalArgumentException, IOException {
        // Extract sub from fileName

        String fileName = getMediaFileName(fileId);

        // Extract sub from fileName
        String[] parts = fileName.split("_|\\.");
        String fileUserId = parts[0];
        String fileType = parts[2];

        if (fileType.equals("container") && !userId.equals(fileUserId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

    }

    public static String getMediaFileName(String id) throws IOException, IllegalArgumentException {
        // Find out all log files
        File dir = new File(MEDIA_DIRECTORY);
        FilenameFilter uploadIdFileFilter = (d, s) -> {
            String[] parts = s.split("_|\\.");

            return parts[1].equals(id);
        };
        String[] fileNames = dir.list(uploadIdFileFilter);

        if (fileNames == null || fileNames.length == 0) {
            throw new IllegalArgumentException("Media not found");
        }

        String fileName = fileNames[0];

        // Get video file from disk
        return fileName;
    }
}
