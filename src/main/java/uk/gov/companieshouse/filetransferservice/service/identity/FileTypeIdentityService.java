package uk.gov.companieshouse.filetransferservice.service.identity;

import java.io.IOException;
import java.util.List;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.logging.Logger;

@Service
public class FileTypeIdentityService {
    
    private final Tika tika;
    private final Logger logger;

    @Autowired
    public FileTypeIdentityService(final Tika tika, final Logger logger) {
        this.tika = tika;
        this.logger = logger;
    }

    public String detectMimeType(final byte[] fileContent) throws IOException {
        if (fileContent == null || fileContent.length == 0) {
            throw new IOException("File content must not be null or empty");
        }
        logger.trace(String.format("Detecting mime type for file content of length %d", fileContent.length));
        logger.info("Starting detect file type");
        
        return tika.detect(fileContent);
    }

    public String detectMimeType(final MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File must not be null or empty");
        }
        logger.trace(String.format("Detecting mime type for file content of length %d", file.getSize()));
        logger.info("Starting detect file type");

        return tika.detect(file.getInputStream());
    }

    /**
     * Check whether the detected mime type matches the mime type implied by the file extension.
     * Returns true if no meaningful extension-derived mime type can be determined.
     * @throws MimeTypeException 
     */
    public List<String> getAllowedExtensions(final String detectedMime) throws MimeTypeException {
        if (detectedMime == null || detectedMime.isBlank()) {
            return List.of();
        }

        final MimeType mimeType = MimeTypes.getDefaultMimeTypes().forName(detectedMime);
        final List<String> exts = mimeType.getExtensions();
        return exts == null ? List.of() : List.copyOf(exts);
    }
}
