package uk.gov.companieshouse.filetransferservice.exception;

import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;

public class FileNotCleanException extends RuntimeException {
    private final AvStatusApi avStatus;
    private final String fileId;

    public FileNotCleanException(AvStatusApi avStatus, String fileId) {
        this.avStatus = avStatus;
        this.fileId = fileId;
    }

    public AvStatusApi getAvStatus() {
        return avStatus;
    }

    public String getFileId() {
        return fileId;
    }
}
