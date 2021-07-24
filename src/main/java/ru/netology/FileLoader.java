package ru.netology;

import java.io.*;

public class FileLoader {
    private static final String PATH_TO_FILE = "D:/JAVA/jspr-hw1-web";

    private FileParam file;
    private BufferedOutputStream bufferedOutputStream;

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
            if (newFile.createNewFile())
                System.out.println("   В каталоге " + PATH_TO_FILE + " создан файл '" + fileName + "'.");
            else if (newFile.exists())
                System.out.println("   В каталоге " + PATH_TO_FILE + " открыт файл '" + fileName + "' .");

            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(newFile, false));

            bufferedOutputStream.write(file.getFileData());
            bufferedOutputStream.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Ошибка при создании файла " + fileName);
        }
    }

    public void close() {
        try {
            bufferedOutputStream.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
