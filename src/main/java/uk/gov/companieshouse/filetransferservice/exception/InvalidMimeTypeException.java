package uk.gov.companieshouse.filetransferservice.exception;

public class InvalidMimeTypeException extends RuntimeException {
    private final String mimeType;

    public InvalidMimeTypeException(String mimeType) {
        super(createMessage(mimeType));
        this.mimeType = mimeType;
    }

    private static String createMessage(String mimeType) {
        return String.format("Mime type [%s] is not a valid mime type", mimeType);
    }

    public String getMimeType() {
        return mimeType;
    }
}

