package ru.netology;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileLoader {
    private static final String PATH_TO_FILE = "D:/JAVA/jspr-hw1-web";

    private FileParam file;
    private BufferedWriter bufferedWriter;

    public FileLoader(FileParam file) {
        this.file = file;
        createFile();
        close();
    }

    public FileParam getFile() {
        return file;
    }

    public void setFile(FileParam file) {
        this.file = file;
    }

    private void createFile() {
        String fileName = file.getFileName();

        File newFile = new File(PATH_TO_FILE + "//" + fileName);
        try {
            if (newFile.createNewFile()) {
                System.out.println("   В каталоге " + PATH_TO_FILE + " создан файл '" + fileName + "'.");
                bufferedWriter = new BufferedWriter(new FileWriter(newFile, false));
            } else if (newFile.exists()) {
                System.out.println("   В каталоге " + PATH_TO_FILE + " открыт файл '" + fileName + "' .");
                bufferedWriter = new BufferedWriter(new FileWriter(newFile, false));
            }

            bufferedWriter.write(file.getFileData());
            bufferedWriter.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Ошибка при создании файла " + fileName);
        }
    }

    public void close() {
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
