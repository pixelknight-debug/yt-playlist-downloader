package com.example.downloader;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @PostMapping("/download")
    public Map<String, String> startDownload(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is required");
        }
        String jobId = downloadService.createAndStartJob(url);
        return Map.of("job_id", jobId, "status", "queued");
    }

    @GetMapping("/status/{jobId}")
    public Map<String, String> getStatus(@PathVariable String jobId) {
        JobStatus status = downloadService.getStatus(jobId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        Map<String, String> result = new HashMap<>();
        result.put("status", status.getStatus());
        if (status.getFilename() != null) result.put("filename", status.getFilename());
        if (status.getError() != null) result.put("error", status.getError());
        return result;
    }

    @GetMapping("/download-file/{jobId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String jobId) {
        Path zipPath = downloadService.getZipPath(jobId);
        if (zipPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not ready");
        }
        JobStatus status = downloadService.getStatus(jobId);
        Resource resource = new FileSystemResource(zipPath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + status.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}