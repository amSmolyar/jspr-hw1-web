package ru.netology;

public class FileParam {
    private String fileType;
    private String fileName;
    private byte[] fileData;
    private int fileDataLength;

    public FileParam() {
        fileType = "";
        fileName = "";
        fileData = new byte[]{};
        fileDataLength = 0;
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

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public int getFileDataLength() {
        return fileDataLength;
    }

    public void setFileDataLength(int fileDataLength) {
        this.fileDataLength = fileDataLength;
    }
}
