package ru.netology;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {
    private String method;
    private String path;
    private Map<String, List<String>> queryParams;
    private Map<String, String> headers;
    private int bodyLength;
    private byte[] body;
    private Map<String, List<String>> postPartParams;
    private FileParam file;

    private Request() {
        this.queryParams = new HashMap<>();
        this.headers = new HashMap<>();
        this.postPartParams = new HashMap<>();
        this.file = new FileParam();
    }

    public static Request getRequest(BufferedInputStream in) throws IOException, NumberFormatException {
        Request request = new Request();
        // лимит на request line + заголовки
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        if (requestLineEnd == -1)
            throw new IOException();

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd));
        request = request.readRequestLineAndQuery(request, requestLine);

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1)
            throw new IOException();

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headersString = new String(headersBytes);
        request = request.readHeaders(request, headersString);

        request.bodyLength = -1;
        if (!request.method.equals("GET")) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            if (request.headers.containsKey("Content-Length")) {
                request.bodyLength = Integer.parseInt(request.headers.get("Content-Length"));
                request.body = in.readNBytes(request.bodyLength);

                if (request.headers.get("Content-Type").equals("application/x-www-form-urlencoded")) {
                    request.postPartParams = request.parsePostParams(request.body);
                } else if (request.headers.get("Content-Type").startsWith("multipart/form-data; boundary=")) {
                    String contentType = request.headers.get("Content-Type");
                    int indDescriptor = contentType.indexOf("=");
                    String boundary = "--" + contentType.substring(indDescriptor + 1);

                    int fileDataLength = 0;
                    if (request.bodyLength > 0) {
                        fileDataLength = getFileLength(request.body, request.bodyLength);
                        request.file.setFileDataLength(fileDataLength);
                    }

                    request.postPartParams = request.getParts(Arrays.copyOfRange(request.body, 0, request.bodyLength - fileDataLength), boundary);
                    request.file.setFileData(Arrays.copyOfRange(request.body, request.bodyLength - fileDataLength, request.bodyLength));
                    request = request.setFileParams(request);
                }
            }
        }

        return request;
    }


    private Request readRequestLineAndQuery(Request request, String requestLine) throws IOException {
        final var allowedMethods = List.of("GET", "POST");

        final var requestLineParts = requestLine.split(" ");
        if (requestLineParts.length != 3)
            throw new IOException();

        request.method = requestLineParts[0];
        if (!allowedMethods.contains(method))
            throw new IOException();

        final var pathWithQueryArray = requestLineParts[1].split("\\?");
        request.path = pathWithQueryArray[0];
        if (!request.path.startsWith("/"))
            throw new IOException();


        if (pathWithQueryArray.length == 2) {
            final String allQuery = pathWithQueryArray[1];
            request.queryParams = parseQueryParams(allQuery);
        }

        return request;
    }

    private Request readHeaders(Request request, String headersStr) {
        String line;
        String headerName;
        String headerValue;
        int indDelimiter;

        final var headersParts = headersStr.split("\r\n");

        for (int ii = 0; ii < headersParts.length; ii++) {
            line = headersParts[ii];
            if (line.equals(""))
                continue;

            indDelimiter = line.indexOf(":");
            headerName = line.substring(0, indDelimiter).trim();
            headerValue = line.substring(indDelimiter + 1).trim();

            request.headers.put(headerName, headerValue);
        }

        return request;
    }

    public List<String> getQueryParam(String name) {
        List<String> value = new ArrayList<>();
        if (queryParams.containsKey(name))
            value = queryParams.get(name);

        return value;
    }

    private Map<String, List<String>> parseQueryParams(String request) {
        return getParamsFromString(request, "\\&");
    }

    public List<String> getPostParam(String name) {
        List<String> value = new ArrayList<>();
        if (postPartParams.containsKey(name))
            value = postPartParams.get(name);

        return value;
    }

    private Map<String, List<String>> parsePostParams(byte[] bodyBytes) {
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        return getParamsFromString(body, "\\&");
    }

    private Map<String, List<String>> getParamsFromString(String str, String boundary) {
        String param;
        String paramName;
        String paramValue;
        int indDelimiter;

        Map<String, List<String>> paramMap = new HashMap<>();

        var paramsArray = str.split(boundary);
        for (int ii = 0; ii < paramsArray.length; ii++) {
            param = paramsArray[ii];
            indDelimiter = param.indexOf("=");
            paramName = param.substring(0, indDelimiter);
            paramValue = param.substring(indDelimiter + 1);

            List<String> values = new ArrayList<>();
            if (paramMap.containsKey(paramName))
                values = paramMap.get(paramName);

            values.add(paramValue);
            paramMap.put(paramName, values);
        }
        return paramMap;
    }

    public List<String> getPart(String name) {
        List<String> value = new ArrayList<>();
        if (postPartParams.containsKey(name))
            value = postPartParams.get(name);

        return value;
    }

    private static int getFileLength(byte[] bodyBytes, int bodyLength) {
        final var paramLineFirstDelimiter = ("Content-Type:").getBytes();
        final var paramLineDelimiter = new byte[]{'\r', '\n', '\r', '\n'};

        final var paramLineFirstDelimiterIndex = indexOf(bodyBytes, paramLineFirstDelimiter, 0, bodyLength);

        final int paramLineEndIndex;
        if (paramLineFirstDelimiterIndex == -1)
            return 0;
        else
            paramLineEndIndex = indexOf(bodyBytes, paramLineDelimiter, paramLineFirstDelimiterIndex, bodyLength);

        return (bodyLength - paramLineEndIndex - paramLineDelimiter.length);
    }

    private Map<String, List<String>> getParts(byte[] bodyBytes, String boundary) {
        Map<String, List<String>> partParams = new HashMap<>();
        String param;
        String partName;
        String partValue;
        int indNameDelimiter;
        int indValDelimiter;

        String formBody = new String(bodyBytes, StandardCharsets.UTF_8);

        var postParamsArray = formBody.split(boundary);
        for (int ii = 0; ii < postParamsArray.length; ii++) {
            param = postParamsArray[ii];
            if (param.equals(""))
                continue;

            indNameDelimiter = param.indexOf("=");
            indValDelimiter = param.indexOf("\r\n\r\n");
            partName = param.substring(indNameDelimiter + 1, indValDelimiter).trim();
            partValue = param.substring(indValDelimiter + 4).trim();

            if (partName.contains("Content-Type:")) {

                var filePartArray = partName.split("\r\n");
                indNameDelimiter = filePartArray[0].indexOf(";");
                String fileNameStr = filePartArray[0].substring(indNameDelimiter + 2);
                partName = fileNameStr.substring(0, fileNameStr.indexOf("=")).trim();
                partValue = fileNameStr.substring(fileNameStr.indexOf("=") + 2, fileNameStr.length() - 1).trim();

                List<String> values = new ArrayList<>();
                values.add(partValue);
                partParams.put(partName, values);

                indNameDelimiter = filePartArray[1].indexOf(":");
                partName = filePartArray[1].substring(0, indNameDelimiter).trim();
                partValue = filePartArray[1].substring(indNameDelimiter + 2).trim();
            } else
                partName = partName.substring(1, partName.length() - 1);

            List<String> values = new ArrayList<>();
            if (partParams.containsKey(partName))
                values = partParams.get(partName);

            values.add(partValue);
            partParams.put(partName, values);
        }
        return partParams;
    }

    private Request setFileParams(Request request) {
        request.file.setFileName(getPart("filename").get(0));
        request.file.setFileType(getPart("Content-Type").get(0));

        return request;
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    public FileParam getFile() {
        return file;
    }
}
