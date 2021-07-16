package ru.netology;

public class FileParam {
    private String fileType;
    private String fileName;
    private String fileData;

    public FileParam() {
        fileType = "";
        fileName = "";
        fileData = "";
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileData() {
        return fileData;
    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }
}
