package uk.gov.companieshouse.filetransferservice.exception;

import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;

public class FileNotCleanException extends Exception {
    private final AvStatusApi avStatus;

    public FileNotCleanException(AvStatusApi avStatus) {
        this.avStatus = avStatus;
    }

    public AvStatusApi getAvStatus() {
        return avStatus;
    }
}
