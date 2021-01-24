package com.laboschqpa.filehost.util;

import org.apache.tomcat.util.http.fileupload.FileItemStream;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class FileUploadUtils {
    private static final Integer UPLOADED_FILE_NAME_MAX_LENGTH = 200;
    private static final DateTimeFormatter unnamedFileNameDatetimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss").withZone(ZoneId.of("UTC"));

    public static String createNormalizedFileName(FileItemStream uploadedFile) {
        final String originalFileName = uploadedFile.getName();

        if (originalFileName == null || originalFileName.isBlank()) {
            return generateNameForUnnamedFile();
        } else {
            String normalizedFileName = originalFileName;

            if (normalizedFileName.contains("/")) {
                final String[] split = normalizedFileName.split("/");
                String buff = split[split.length - 1];
                if (buff.isBlank()) {
                    normalizedFileName = normalizedFileName.replace("/", "");
                } else {
                    normalizedFileName = buff;
                }
            }
            if (normalizedFileName.contains("\\")) {
                final String[] split = normalizedFileName.split("\\\\");
                String buff = split[split.length - 1];
                if (buff.isBlank()) {
                    normalizedFileName = normalizedFileName.replace("\\", "");
                } else {
                    normalizedFileName = buff;
                }
            }

            normalizedFileName = normalizedFileName.trim();

            if (normalizedFileName.length() > UPLOADED_FILE_NAME_MAX_LENGTH) {
                normalizedFileName = normalizedFileName.substring(normalizedFileName.length() - UPLOADED_FILE_NAME_MAX_LENGTH);
            }

            if (normalizedFileName.isBlank()) {
                return generateNameForUnnamedFile();
            }
            return normalizedFileName;
        }
    }

    private static String generateNameForUnnamedFile() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        return String.format("unnamed_%s.file", zonedDateTime.format(unnamedFileNameDatetimeFormatter));
    }
}
