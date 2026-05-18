package uk.gov.companieshouse.filetransferservice.exception;

public class MismatchingContentTypeException extends RuntimeException {
    private final String expectedType;
    private final String actualType;

    public MismatchingContentTypeException(String expectedType, String actualType) {
        super(createMessage(expectedType, actualType));
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    private static String createMessage(String expectedType, String actualType) {
        return String.format("Expected type [%s] does not match actual type [%s]", expectedType, actualType);
    }

    public String getExpectedType() {
        return expectedType;
    }

    public String getActualType() {
        return actualType;
    }
    
}
