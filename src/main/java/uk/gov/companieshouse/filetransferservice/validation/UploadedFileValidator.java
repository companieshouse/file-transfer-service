package uk.gov.companieshouse.filetransferservice.validation;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;

import java.util.Arrays;
import java.util.List;

@Component
public class UploadedFileValidator {
    public static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "image/gif",
            "application/x-rar-compressed",
            "application/x-tar",
            "application/x-7z-compressed",
            "application/xhtml+xml",
            "application/zip"
    );

    private boolean isValidMimeType(final String mimeType) {
        return ALLOWED_MIME_TYPES.contains(mimeType);
    }

    public void validate(FileApi file) throws InvalidMimeTypeException {
        var contentType = file.getMimeType();
        if (!isValidMimeType(contentType)) {
            throw new InvalidMimeTypeException(contentType);
        }
    }
}
