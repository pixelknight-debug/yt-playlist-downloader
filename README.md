# 🎵 YouTube Playlist Downloader

A full-stack web application to download YouTube and YouTube Music playlists as MP3 files.

## Features
* Built with Java Spring Boot and standard HTML/JS.
* Uses yt-dlp and FFmpeg to extract high-quality audio.
* Zips the final MP3s for easy downloading.

## How to Run
1. Install Java 17+, yt-dlp, and FFmpeg.
2. Run `mvnw.cmd spring-boot:run` (Windows) or `./mvnw spring-boot:run` (Mac/Linux).
3. Go to http://localhost:8080 in your browser.

## Note (VERY IMPORTANT)
This app extracts cookies from Firefox (`--cookies-from-browser firefox`) to bypass bot detection. Make sure you are logged into YouTube on Firefox.