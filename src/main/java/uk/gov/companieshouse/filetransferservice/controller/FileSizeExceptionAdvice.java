package uk.gov.companieshouse.filetransferservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;

/**
 * A controller advice that handles {@link MaxUploadSizeExceededException} exceptions.
 *
 * This advice is necessary because the exception will be thrown before it reaches the controller, so it cannot be
 * handled there. The maximum upload size is configured by the {@code spring.servlet.multipart.max-file-size} property,
 * which is in turn configured by the {@code MAX_FILE_SIZE} environment variable.
 *
 * When a {@code MaxUploadSizeExceededException} is thrown, this advice logs an error message and returns a
 * {@code ResponseEntity} with an HTTP status code of {@link HttpStatus#PAYLOAD_TOO_LARGE} and a message
 * indicating that the uploaded file is too large.
 */

@ControllerAdvice
public class FileSizeExceptionAdvice {
    final Logger logger;

    @Autowired
    public FileSizeExceptionAdvice(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Handles {@link MaxUploadSizeExceededException} exceptions by logging an error message and returning a
     * {@code ResponseEntity} with an HTTP status code of {@link HttpStatus#PAYLOAD_TOO_LARGE} and a message
     * indicating that the uploaded file is too large.
     *
     * @param exception the {@code MaxUploadSizeExceededException} to handle
     * @return a {@code ResponseEntity} with an HTTP status code of {@link HttpStatus#PAYLOAD_TOO_LARGE}
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> handleFileTooLarge(MaxUploadSizeExceededException exception) {
        var loggedVars = new HashMap<String, Object>();
        loggedVars.put("maxFileSize", exception.getMaxUploadSize());
        logger.error("Uploaded file was too large", exception, loggedVars);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }
}
