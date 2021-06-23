package ru.netology;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static final int PORT = 9999;
    public static final int CNT_THREAD_POOL = 64;

    public static void main(String[] args) throws IOException {
        Server server = new Server(PORT);

        // ===      /classic.html handler
        server.addHandler("GET", "/classic.html", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                // special case for classic
                final var filePath = Path.of(".", "public", request.getPath());
                final var mimeType = Files.probeContentType(filePath);
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();
                server.defaultResponse(request, responseStream);
            }
        });

        // ===      /resources.html handler
        server.addHandler("GET", "/resources.html", (Request request, BufferedOutputStream responseStream) -> server.defaultResponse(request, responseStream));

        server.runServer(CNT_THREAD_POOL);
    }
}


