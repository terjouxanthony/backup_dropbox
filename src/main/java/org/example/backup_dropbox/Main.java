package org.example.backup_dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException, DbxException {
        final long startNanos = System.nanoTime();

        var mapper = new ObjectMapper(new YAMLFactory());
        final Config config = mapper.readValue(new File("./config.yaml"), Config.class);
        var dateFormatter = DateTimeFormatter.ofPattern(config.dateFormat());

        // Create Dropbox client
        DbxClientV2 client = new DbxClientV2(
                DbxRequestConfig.newBuilder(config.dropboxClientIdentifier()).build(),
                config.dropbox_token());

        final List<Path> existingLocalFiles = listLocalFiles(config.backupDir());
        final List<String> existingDropboxFiles = listDropboxFiles(client);

        for (String prefix : config.filePrefixToBackup()) {
            final String prefixLower = prefix.toLowerCase();
            final List<DropboxPathAndDate> dropboxBackupFiles = existingDropboxFiles.stream()
                    .map(file -> file.substring(1)) // remove leading slash
                    .filter(file -> file.startsWith(prefixLower))
                    .map(file -> new DropboxPathAndDate(file, parseDate(file, prefixLower, dateFormatter, config)))
                    .collect(Collectors.toList());

            log.info("{} Backups detected for {}", dropboxBackupFiles.size(), prefix);

            // if there are too many files in dropbox, delete them
            if (dropboxBackupFiles.size() >= config.numberOfDaysToKeep()) {
                dropboxBackupFiles.sort(Comparator.comparing(DropboxPathAndDate::date).reversed());
                for (var filetoDelete : dropboxBackupFiles.subList(config.numberOfDaysToKeep() - 1, dropboxBackupFiles.size())) {
                    deleteDropboxFile(client, "/" + filetoDelete.path());
                    log.info("Deleted file {}", filetoDelete.path());
                }
            }

            // get most recent local file and push it to dropbox
            final Optional<LocalPathAndDate> mostRecentBackup = existingLocalFiles.stream()
                    .flatMap(path -> {
                        String pathLower = path.getFileName().toString().toLowerCase();
                        if (pathLower.startsWith(prefixLower)) {
                            return Stream.of(new LocalPathAndDate(path, pathLower,
                                    parseDate(pathLower, prefixLower, dateFormatter, config)));
                        }
                        return Stream.empty();
                    })
                    .max(Comparator.comparing(LocalPathAndDate::date));

            if (mostRecentBackup.isPresent()) {
                var fileToAdd = mostRecentBackup.get();
                try (InputStream in = Files.newInputStream(fileToAdd.path())) {
                    uploadDropboxFile(client, "/" + fileToAdd.pathLower(), in);
                    log.info("Uploaded file {}", fileToAdd.path());
                }
            }
        }

        final long endNanos = System.nanoTime();
        log.info("Synchronization with Dropbox took {} ms", Duration.ofNanos(endNanos - startNanos).toMillis());
    }

    private static LocalDateTime parseDate(String file,
                                           String prefixLower,
                                           DateTimeFormatter dateFormatter,
                                           Config config) {
        final String dateStr = file.substring(prefixLower.length(), prefixLower.length() + config.dateFormat().length());
        try {
            return LocalDateTime.parse(dateStr, dateFormatter);
        } catch (DateTimeParseException ex) {
            throw new RuntimeException("The parameter date_format in your config is invalid", ex);
        }
    }

    private static List<Path> listLocalFiles(String path) throws IOException {
        try (var files = Files.list(Paths.get(path))) {
            return files.toList();
        }
    }

    private static FileMetadata uploadDropboxFile(DbxClientV2 client, String path, InputStream in) throws DbxException, IOException {
        return client.files().uploadBuilder(path).uploadAndFinish(in);
    }

    private static void deleteDropboxFile(DbxClientV2 client, String path) throws DbxException {
        client.files().deleteV2(path);
    }

    static List<String> listDropboxFiles(DbxClientV2 client) throws DbxException {
        List<String> existingFilePaths = new ArrayList<>();
        ListFolderResult result = client.files().listFolder("");
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                existingFilePaths.add(metadata.getPathLower());
            }
            if (!result.getHasMore()) {
                break;
            }
            result = client.files().listFolderContinue(result.getCursor());
        }
        return existingFilePaths;
    }

    record DropboxPathAndDate(String path, LocalDateTime date) {
    }

    record LocalPathAndDate(Path path, String pathLower, LocalDateTime date) {
    }

    record Config(
            @JsonProperty("backup_dir")
            String backupDir,
            @JsonProperty("file_prefix_to_backup")
            List<String> filePrefixToBackup,
            @JsonProperty("date_format")
            String dateFormat,
            @JsonProperty("dropbox_client_identifier")
            String dropboxClientIdentifier,
            @JsonProperty("dropbox_token")
            String dropbox_token,
            @JsonProperty("number_of_days_to_keep")
            int numberOfDaysToKeep
    ) {
    }
}
