package uk.gov.companieshouse.filetransferservice.validation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tika.mime.MimeTypeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.exception.MismatchFileExtensionException;
import uk.gov.companieshouse.filetransferservice.exception.MismatchingContentTypeException;
import uk.gov.companieshouse.filetransferservice.service.identity.FileTypeIdentityService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.filetransferservice.model.legacy.FileApi;

@Component
public class MimeTypeValidator {

    private static final String REQUESTED_MIME_TYPE = "requested_mime_type";
    
    private static final String DETECTED_MIME_TYPE = "detected_mime_type";

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

    public void validate(final FileApi file) throws InvalidMimeTypeException, MismatchingContentTypeException, IOException, MimeTypeException {
        final String mimeType = file.getMimeType();
        final String detectedMimeType = fileTypeIdentityService.detectMimeType(file.getBody());
        final String fileExtension = getFileExtension(file.getFileName());

        Map<String, Object> logMap = new HashMap<>();
        logMap.put(REQUESTED_MIME_TYPE, mimeType);
        logMap.put(DETECTED_MIME_TYPE, detectedMimeType);

        logger.info("Validating file mime types", logMap);

        validate(mimeType, detectedMimeType, fileExtension);
    }
    
    public void validate(final MultipartFile file) throws IOException, InvalidMimeTypeException, MismatchingContentTypeException, MimeTypeException {
        final String mimeType = file.getContentType();
        final String detectedMimeType = fileTypeIdentityService.detectMimeType(file);
        final String fileExtension = getFileExtension(file.getOriginalFilename());

        Map<String, Object> logMap = new HashMap<>();
        logMap.put(REQUESTED_MIME_TYPE, mimeType);
        logMap.put(DETECTED_MIME_TYPE, detectedMimeType);

        logger.info("Validating file mime types", logMap);

        validate(mimeType, detectedMimeType, fileExtension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidMimeTypeException("File name is missing or blank");
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            throw new InvalidMimeTypeException("File name does not have a valid extension: " + fileName);
        }
        return fileName.substring(lastDotIndex);
    }
    
    private void validate(final String mimeType, final String detectedMimeType, final String fileExtension) throws InvalidMimeTypeException, MismatchingContentTypeException, MimeTypeException {
        
        final String normalizedMimeType = mimeType != null ? mimeType.toLowerCase() : null;
        final String normalizedDetectedMimeType = detectedMimeType != null ? detectedMimeType.toLowerCase() : null;
        final String normalizedFileExtension = fileExtension != null ? fileExtension.toLowerCase() : null;

        if (mimeType == null || mimeType.isBlank()) {
            throw new InvalidMimeTypeException("No mime type provided");
        }
        
        if (!ALLOWED_MIME_TYPES.contains(normalizedMimeType)) {
            throw new InvalidMimeTypeException(mimeType);
        }
        
        if (detectedMimeType == null || !ALLOWED_MIME_TYPES.contains(normalizedDetectedMimeType)) {
            throw new InvalidMimeTypeException(String.format("The file content does not match the allowed mime type. Detected: %s", detectedMimeType));
        }
        
        final List<String> allowedExtensions = fileTypeIdentityService.getAllowedExtensions(detectedMimeType);

        if(!allowedExtensions.contains(normalizedFileExtension)) {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put(DETECTED_MIME_TYPE, detectedMimeType);
            logMap.put("file_extension", fileExtension);
            logMap.put("allowed_extensions", allowedExtensions);
            logger.debug("File extension did not match the detected mime type", logMap);
            throw new MismatchFileExtensionException(String.format("The file extension %s does not match the allowed mime type. Detected: %s", fileExtension, detectedMimeType));
        }
        
        if (!mimeType.equalsIgnoreCase(detectedMimeType)) {
            throw new MismatchingContentTypeException(mimeType, detectedMimeType);
        }

        logger.debug("Accepted file type submitted", Map.of("mime_type", mimeType));
    }

}
