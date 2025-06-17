package uk.gov.companieshouse.filetransferservice.validation;

import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.logging.Logger;

@Component
public class MimeTypeValidator {

    private final Logger logger;

    public MimeTypeValidator(final Logger logger){
        this.logger = logger;
    }

    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
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
            "application/vnd.rar",
            "application/x-7z-compressed",
            "application/xhtml+xml",
            "application/zip",
            "application/xml",
            "multipart/x-zip",
            "application/octet-stream",
            "application/zip-compressed",
            "application/x-zip-compressed"
    );

    public void validate(final String mimeType) throws InvalidMimeTypeException {
        logger.trace("Validating mime type " + mimeType);

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new InvalidMimeTypeException(mimeType);
        }

        logger.debug(String.format("Accepted file type submitted: %s", mimeType));
    }
}
