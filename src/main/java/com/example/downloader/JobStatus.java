package com.example.downloader;

public class JobStatus {
    private String status;
    private String filename;
    private String error;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}