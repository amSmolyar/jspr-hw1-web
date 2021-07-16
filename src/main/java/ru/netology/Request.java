package ru.netology;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Request {
    private String method;
    private String path;
    private Map<String, List<String>> queryParams;
    private Map<String, String> headers;
    private String body;
    private Map<String, List<String>> postPartParams;
    private FileParam file;


    private static FileParam fileParam;


    private Request(String method, String path, Map<String, List<String>> queryParams, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = "";
        this.postPartParams = new HashMap<>();
        this.file = new FileParam();
    }

    private Request(String method, String path, Map<String, List<String>> queryParams, Map<String, String> headers, String body, Map<String, List<String>> postPartParams) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
        this.postPartParams = postPartParams;
        this.file = new FileParam();
    }

    private Request(String method, String path, Map<String, List<String>> queryParams, Map<String, String> headers, String body, Map<String, List<String>> postPartParams, FileParam file) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
        this.postPartParams = postPartParams;
        this.file = file;
    }

    public static Request getRequest(BufferedReader in) throws IOException, NumberFormatException {
        Request request;
        // =============  requestLine  =============
        var requestLine = in.readLine();
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            throw new IOException();
        }

        final var method = parts[0];

        final var pathWithQueryArray = parts[1].split("\\?");
        final var path = pathWithQueryArray[0];


        Map<String, List<String>> queryParams = new HashMap<>();
        if (pathWithQueryArray.length == 2) {
            final String allQuery = pathWithQueryArray[1];
            queryParams = parseQueryParams(allQuery);
        }

        // =============  headers  =============
        Map<String, String> headers = new HashMap<>();
        String headerName;
        String headerValue;

        int bodyLength = -1;
        while (!in.ready()) {}
        while (!(requestLine = in.readLine().trim()).equals("")) {
            int indDelimiter = requestLine.indexOf(":");
            headerName = requestLine.substring(0, indDelimiter).trim();
            headerValue = requestLine.substring(indDelimiter + 1).trim();

            if (headerName.equals("Content-Length")) {
                bodyLength = Integer.parseInt(headerValue);
            }

            headers.put(headerName, headerValue);

            while (!in.ready()) {}
        }

        Map<String, List<String>> postPartParams = new HashMap<>();
        String body;
        if (!method.equals("GET") && (bodyLength > 0) && (headers.containsKey("Content-Type"))) {
            body = readRequestBody(in, bodyLength);

            if (headers.get("Content-Type").equals("application/x-www-form-urlencoded")) {
                postPartParams = parsePostParams(body);
                request = new Request(method, path, queryParams, headers, body, postPartParams);
            } else if (headers.get("Content-Type").startsWith("multipart/form-data; boundary=")) {
                String contentType = headers.get("Content-Type");
                int indDescriptor = contentType.indexOf("=");
                String boundary = "--" + contentType.substring(indDescriptor + 1);
                postPartParams = getParts(body, boundary);

                request = new Request(method, path, queryParams, headers, body, postPartParams, fileParam);
            } else
                request = new Request(method, path, queryParams, headers);
        } else
            request = new Request(method, path, queryParams, headers);

        return request;
    }

    private static String readRequestBody(BufferedReader in, int bodyLength) throws IOException {
        // =============  body  =============
        ByteArrayOutputStream bodyBAOStream = new ByteArrayOutputStream();
        StringBuilder stringBuilder = new StringBuilder();
        int size = 0;
        int cntWait = 0;
        while ((bodyBAOStream.size() < bodyLength)) {
            if (!in.ready()) {
                cntWait++;
                if (cntWait == 10)
                    break;
            }

            bodyBAOStream.write(in.read());
        }
        size += bodyBAOStream.size();
        byte[] byteArray = bodyBAOStream.toByteArray();

        stringBuilder.append(new String(byteArray, StandardCharsets.UTF_8));
        bodyBAOStream.reset();

        return stringBuilder.toString();
    }

    public List<String> getQueryParam(String name) {
        List<String> value = new ArrayList<>();
        if (queryParams.containsKey(name))
            value = queryParams.get(name);

        return value;
    }

    private static Map<String, List<String>> parseQueryParams(String request) {
        Map<String, List<String>> queryParams = new HashMap<>();
        String query;
        String queryName;
        String queryValue;
        int indDelimiter;


        var queryArray = request.split("\\&");
        for (int ii = 0; ii < queryArray.length; ii++) {
            query = queryArray[ii];
            indDelimiter = query.indexOf("=");
            queryName = query.substring(0, indDelimiter);
            queryValue = query.substring(indDelimiter + 1);

            if (queryParams.containsKey(queryName)) {
                var values = queryParams.get(queryName);
                values.add(queryValue);
                queryParams.put(queryName, values);
            } else {
                List<String> values = new ArrayList<>();
                values.add(queryValue);
                queryParams.put(queryName, values);
            }
        }
        return queryParams;
    }

    public List<String> getPostParam(String name) {
        List<String> value = new ArrayList<>();
        if (postPartParams.containsKey(name))
            value = postPartParams.get(name);

        return value;
    }

    private static Map<String, List<String>> parsePostParams(String body) {
        Map<String, List<String>> postParams = new HashMap<>();
        String param;
        String postName;
        String postValue;
        int indDelimiter;

        var postParamsArray = body.split("\\&");
        for (int ii = 0; ii < postParamsArray.length; ii++) {
            param = postParamsArray[ii];
            indDelimiter = param.indexOf("=");
            postName = param.substring(0, indDelimiter);
            postValue = param.substring(indDelimiter + 1);

            if (postParams.containsKey(postName)) {
                var values = postParams.get(postName);
                values.add(postValue);
                postParams.put(postName, values);
            } else {
                List<String> values = new ArrayList<>();
                values.add(postValue);
                postParams.put(postName, values);
            }
        }
        return postParams;
    }

    public List<String> getPart(String name) {
        List<String> value = new ArrayList<>();
        if (postPartParams.containsKey(name))
            value = postPartParams.get(name);

        return value;
    }

    private static Map<String, List<String>> getParts(String body, String boundary) {
        Map<String, List<String>> partParams = new HashMap<>();
        String param;
        String partName;
        String partValue;
        int indNameDelimiter;
        int indValDelimiter;

        var postParamsArray = body.split(boundary);
        for (int ii = 0; ii < (postParamsArray.length - 1); ii++) {
            param = postParamsArray[ii].trim();
            if (param.equals(""))
                continue;

            indNameDelimiter = param.indexOf("=");
            indValDelimiter = param.indexOf("\r\n\r\n");
            partName = param.substring(indNameDelimiter + 1, indValDelimiter);
            partValue = param.substring(indValDelimiter + 4);

            if (partName.contains("Content-Type:")) {
                fileParam = new FileParam();
                fileParam.setFileData(partValue);
                var filePartArray = partName.split("\r\n");
                indNameDelimiter = filePartArray[0].indexOf(";");
                partName = filePartArray[0].substring(1, indNameDelimiter - 1);
                partValue = filePartArray[0].substring(indNameDelimiter + 2);
                fileParam.setFileName(partValue.substring(10, partValue.length() - 1));
                fileParam.setFileType(filePartArray[1].substring(filePartArray[1].indexOf("/") + 1));
            } else
                partName = partName.substring(1, partName.length() - 1);

            if (partParams.containsKey(partName)) {
                var values = partParams.get(partName);
                values.add(partValue);
                partParams.put(partName, values);
            } else {
                List<String> values = new ArrayList<>();
                values.add(partValue);
                partParams.put(partName, values);
            }
        }
        return partParams;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    public FileParam getFile() {
        return file;
    }
}
