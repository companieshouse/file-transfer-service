package uk.gov.companieshouse.filetransferservice.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.logging.Logger;

import java.util.Arrays;
import java.util.List;

@Component
public class UploadedFileValidator {

    private final Logger logger;

    @Autowired
    public UploadedFileValidator(final Logger logger){
        this.logger = logger;
    }

    public static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf",
            "text/csv",
            "text/html",
            "text/xml",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "image/gif",
            "application/x-rar-compressed",
            "application/x-tar",
            "application/x-7z-compressed",
            "application/xhtml+xml",
            "application/zip",
            "multipart/x-zip",
            "application/octet-stream",
            "application/zip-compressed",
            "application/x-zip-compressed"
    );

    private boolean isValidMimeType(final String mimeType) {
        return ALLOWED_MIME_TYPES.contains(mimeType);
    }

    public void validate(FileApi file) throws InvalidMimeTypeException {
        var contentType = file.getMimeType();
        if (isValidMimeType(contentType)) {
            logger.debug(String.format("Accepted file type submitted: %s", contentType));
        } else {
            throw new InvalidMimeTypeException(contentType);
        }
    }
}
