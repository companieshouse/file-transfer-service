package uk.gov.companieshouse.filetransferservice.validation;

import java.io.IOException;
import java.util.Set;

import org.apache.tika.mime.MimeTypeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.exception.MismatchFileExtensionException;
import uk.gov.companieshouse.filetransferservice.exception.MismatchingContentTypeException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidFileTypeException;
import uk.gov.companieshouse.filetransferservice.service.identity.FileTypeIdentityService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.filetransferservice.model.legacy.FileApi;

@Component
public class MimeTypeValidator {

    private final Logger logger;

    private final FileTypeIdentityService fileTypeIdentityService;

    public MimeTypeValidator(final Logger logger, final FileTypeIdentityService fileTypeIdentityService) {
        this.logger = logger;
        this.fileTypeIdentityService = fileTypeIdentityService;
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

    public void validate(final FileApi file) throws InvalidMimeTypeException, IOException, MimeTypeException {
        final String mimeType = file.getMimeType();
        final String detectedMimeType = fileTypeIdentityService.detectMimeType(file.getBody());
        final String fileExtension = file.getFileName().substring(file.getFileName().lastIndexOf('.'));

        logger.info(String.format("Validating file required mime type: %s and detected mime type: %s", mimeType, detectedMimeType));

        validate(mimeType, detectedMimeType, fileExtension);
    }
    
    public void validate(final MultipartFile file) throws IOException, MimeTypeException {
        final String mimeType = file.getContentType();
        final String detectedMimeType = fileTypeIdentityService.detectMimeType(file);
        final String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        logger.info(String.format("Validating file required mime type: %s and detected mime type: %s", mimeType, detectedMimeType));

        validate(mimeType, detectedMimeType, fileExtension);
    }
    
    private void validate(final String mimeType, final String detectedMimeType, final String fileExtension) throws InvalidMimeTypeException, MismatchingContentTypeException, InvalidFileTypeException, MimeTypeException {

        
        if (mimeType == null || mimeType.isBlank()) {
            throw new InvalidMimeTypeException("No mime type provided");
        }
        
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new InvalidMimeTypeException(mimeType);
        }
        
        if (detectedMimeType == null || !ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new InvalidFileTypeException(String.format("The file content does not match the allowed mime type. Detected: %s", detectedMimeType));
        }
        
        if(!fileTypeIdentityService.getAllowedExtensions(detectedMimeType).contains(fileExtension)) {
            throw new MismatchFileExtensionException(String.format("The file extension %s does not match the allowed mime type. Detected: %s", fileExtension, detectedMimeType));
        }
        
        if (!mimeType.equals(detectedMimeType)) {
            throw new MismatchingContentTypeException(mimeType, detectedMimeType);
        }

        logger.debug(String.format("Accepted file type submitted: %s", mimeType));
    }

}
