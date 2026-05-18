package uk.gov.companieshouse.filetransferservice.exception;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException() {
        super("The file content does not match the expected mime type");
    }

    public InvalidFileTypeException(String message) {
        super(message);
    }
}
