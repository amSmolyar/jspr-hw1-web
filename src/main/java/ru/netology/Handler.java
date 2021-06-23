package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@FunctionalInterface
public interface Handler {
    void handle(Request request, BufferedOutputStream responseStream) throws IOException;
}
