package ru.netology;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private String method;
    private String path;
    private Map<String, List<String>> queryParams;
    private Map<String, String> headers;
    private String body;


    private Request(String method, String path, Map<String, List<String>> queryParams, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = "";
    }

    private Request(String method, String path, Map<String, List<String>> queryParams, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
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


        byte[] bodyByteArray;
        if (!method.equals("GET") && (bodyLength > 0)) {
            bodyByteArray = readRequestBody(in, bodyLength);
            request = new Request(method, path, queryParams, headers, bodyByteArray.toString());
        } else
            request = new Request(method, path, queryParams, headers);

        return request;
    }

    private static byte[] readRequestBody(BufferedReader in, int bodyLength) throws IOException {
        // =============  body  =============
        ByteArrayOutputStream bodyBAOStream = new ByteArrayOutputStream();
        while (bodyBAOStream.size() < bodyLength) {
            while (!in.ready()) {}
            bodyBAOStream.write(in.read());
        }
        return bodyBAOStream.toByteArray();
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
}
