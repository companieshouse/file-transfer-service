package uk.gov.companieshouse.filetransferservice.exception;

public class FileNotFoundException extends RuntimeException {
    private final String fileId;

    public FileNotFoundException(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }
}
