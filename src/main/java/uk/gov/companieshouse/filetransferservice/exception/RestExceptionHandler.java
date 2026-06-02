package uk.gov.companieshouse.filetransferservice.exception;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import uk.gov.companieshouse.api.error.ApiErrorResponse;
import uk.gov.companieshouse.filetransferservice.errors.ErrorResponseBuilder;
import uk.gov.companieshouse.logging.Logger;

@ControllerAdvice
public class RestExceptionHandler {

    public static final String FILE_ID_KEY = "fileId";

    private final Logger logger;

    @Autowired
    public RestExceptionHandler(final Logger logger) {
        this.logger = logger;
    }

    @ExceptionHandler({IOException.class})
    public ResponseEntity<ApiErrorResponse> handleIOException(IOException e) {
        logger.error("Error uploading file IOException when reading file contents.", e);

        return ErrorResponseBuilder
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .withError("Unable to upload file",
                        "getBytes",
                        "method",
                        "upload")
                .build();
    }

    @ExceptionHandler({InvalidMimeTypeException.class})
    public ResponseEntity<ApiErrorResponse> handleInvalidMimeType(InvalidMimeTypeException e) {
        logger.error("File was uploaded with an invalid mime type", e);
        return ErrorResponseBuilder
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .withError("Invalid MIME type",
                        "file",
                        "body_parameter",
                        "validation"
                )
                .build();
    }

    @ExceptionHandler({FileNotCleanException.class})
    public ResponseEntity<ApiErrorResponse> handleFileNotCleanException(FileNotCleanException e) {
        String fileId = e.getFileId();

        Map<String, Object> loggedVars = new HashMap<>();
        loggedVars.put(FILE_ID_KEY, fileId);
        logger.infoContext(fileId, "Request for file denied as AV status is not clean", loggedVars);

        return ErrorResponseBuilder
                .status(HttpStatus.FORBIDDEN)
                .withError("File retrieval denied due to unclean antivirus status",
                        fileId,
                        FILE_ID_KEY,
                        "retrieval")
                .build();
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleFileNotFoundException(FileNotFoundException e) {
        String fileId = e.getFileId();

        Map<String, Object> loggedVars = new HashMap<>();
        loggedVars.put(FILE_ID_KEY, fileId);
        logger.errorContext(fileId, "Unable to find file with ID", e, loggedVars);

        return ErrorResponseBuilder
                .status(HttpStatus.NOT_FOUND)
                .withError(String.format("Unable to find file with id [%s]", fileId),
                        fileId,
                        "jsonPath",
                        "retrieval")
                .build();
    }

    /**
     * Handles {@link MaxUploadSizeExceededException} exceptions by logging an error message and returning a
     * {@code ResponseEntity} with an HTTP status code of {@link HttpStatus#PAYLOAD_TOO_LARGE} and a message
     * indicating that the uploaded file is too large.
     *
     * @param e the {@code MaxUploadSizeExceededException} to handle
     * @return a {@code ResponseEntity} with an HTTP status code of {@link HttpStatus#PAYLOAD_TOO_LARGE}
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        Map<String, Object> loggedVars = new HashMap<>();
        loggedVars.put("maxFileSize", e.getMaxUploadSize());
        logger.error("Uploaded file was too large", e, loggedVars);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }

}
