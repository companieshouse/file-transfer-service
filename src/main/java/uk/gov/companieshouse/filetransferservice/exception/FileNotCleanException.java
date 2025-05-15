package uk.gov.companieshouse.filetransferservice.exception;

import uk.gov.companieshouse.api.filetransfer.AvStatus;

public class FileNotCleanException extends RuntimeException {

    private final AvStatus avStatus;
    private final String fileId;

    public FileNotCleanException(AvStatus avStatus, String fileId) {
        this.avStatus = avStatus;
        this.fileId = fileId;
    }

    public AvStatus getAvStatus() {
        return avStatus;
    }

    public String getFileId() {
        return fileId;
    }
}
