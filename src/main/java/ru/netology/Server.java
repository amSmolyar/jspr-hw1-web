package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.swing.text.html.FormSubmitEvent.MethodType.POST;

public class Server {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final List<String> allowedMethods = List.of("GET", "POST");

    private final ServerSocket serverSocket;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> mapHandler;
    private ConcurrentHashMap<String, Handler> entryMap;
    private Request request;

    public Server(int portNumber) throws IOException {
        serverSocket = new ServerSocket(portNumber);
        mapHandler = new ConcurrentHashMap<>();
        entryMap = new ConcurrentHashMap<>();
    }

    public void runServer(int cntPool) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(cntPool);

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                executorService.submit(() -> requestServer(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
            serverSocket.close();
        }
    }

    private void requestServer(Socket socket) {
        try (final var in = new BufferedInputStream(socket.getInputStream());
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            Request request = Request.getRequest(in);

            if (!allowedMethods.contains(request.getMethod())) {
                badRequest(out);
                return;
            }

            if (!request.getPath().startsWith("/")) {
                badRequest(out);
                return;
            }

            if (mapHandler.containsKey(request.getMethod()) && mapHandler.get(request.getMethod()).containsKey(request.getPath())) {
                mapHandler.get(request.getMethod()).get(request.getPath()).handle(request, out);
                socket.close();
            } else if (validPaths.contains(request.getPath())) {
                defaultResponse(request, out);
            } else {
                okResponse(out);
            }

            FileParam file = request.getFile();
            if (!(file.getFileName().equals("") || file.getFileType().equals("") || file.getFileData().equals(""))) {
                FileLoader fileLoader = new FileLoader(file);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notFoundError(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        entryMap.put(path, handler);
        mapHandler.put(method, entryMap);
    }

    public void defaultResponse(Request request, BufferedOutputStream responseStream) throws IOException {
        final var filePath = Path.of(".", "public", request.getPath());
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);
        responseStream.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, responseStream);
        responseStream.flush();
    }

    public void okResponse(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}