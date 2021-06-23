package ru.netology;

public class Request {
    private String method;
    private String path;
    private String headers;
    private String body;

    public Request(String method, String path, String headers) {
        this.method = method;
        this.path = path;
        this.headers = headers;
    }

    public Request(String method,String path, String headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
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

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
