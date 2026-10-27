package com.example.downloader;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DownloadService {

    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public String createAndStartJob(String url) {
        String jobId = UUID.randomUUID().toString();
        JobStatus status = new JobStatus();
        status.setStatus("queued");
        jobs.put(jobId, status);
        executor.submit(() -> processPlaylist(jobId, url));
        return jobId;
    }

    public JobStatus getStatus(String jobId) {
        return jobs.get(jobId);
    }

    public Path getZipPath(String jobId) {
        JobStatus job = jobs.get(jobId);
        if (job == null || !"completed".equals(job.getStatus())) return null;
        Path zipPath = Path.of("./downloads", jobId, job.getFilename());
        return Files.exists(zipPath) ? zipPath : null;
    }

    private void processPlaylist(String jobId, String url) {
        JobStatus status = jobs.get(jobId);
        Path downloadDir = Path.of("./downloads", jobId);

        try {
            Files.createDirectories(downloadDir);
            status.setStatus("downloading");

            String basePath = downloadDir.toAbsolutePath().toString().replace("\\", "/");
            String outputTemplate = basePath + "/%(playlist)s/%(playlist_index)s - %(title)s.%(ext)s";

            // The updated command using Firefox cookies
            List<String> command = List.of(
                    "yt-dlp",
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", "192K",
                    "-o", outputTemplate,
                    "--ignore-errors",
                    "--no-warnings",
                    "--sleep-interval", "3",            // Wait 3 seconds between songs
                    "--max-sleep-interval", "6",        // Up to 6 seconds to look human
                    "--cookies-from-browser", "firefox",// Borrows your Firefox login
                    url
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[yt-dlp] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                status.setStatus("failed");
                status.setError("yt-dlp exited with code " + exitCode);
                return;
            }

            status.setStatus("packaging");

            Path playlistDir;
            try (Stream<Path> stream = Files.list(downloadDir)) {
                playlistDir = stream.filter(Files::isDirectory).findFirst().orElse(null);
            }
            if (playlistDir == null) {
                status.setStatus("failed");
                status.setError("No audio files were downloaded.");
                return;
            }

            String playlistTitle = playlistDir.getFileName().toString();
            String zipName = playlistTitle + ".zip";
            Path zipPath = downloadDir.resolve(zipName);

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath));
                 Stream<Path> files = Files.walk(playlistDir)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        String entryName = downloadDir.relativize(file).toString().replace("\\", "/");
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            status.setFilename(zipName);
            status.setStatus("completed");

        } catch (Exception e) {
            status.setStatus("failed");
            status.setError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }
}