package ru.netology;

import java.io.*;

public class Main {
    public static final int PORT = 9999;
    public static final int CNT_THREAD_POOL = 64;

    public static void main(String[] args) throws IOException {
        Server server = new Server(PORT);
        server.runServer(CNT_THREAD_POOL);
    }
}


