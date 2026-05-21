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
     * Returns a list of allowed file extensions for a given detected mime type. 
     * If the detected mime type is null, blank, or unknown, an empty list is returned.
     * @param detectedMime the detected mime type for a file
     * @return a list of allowed file extensions for the given detected mime type, or an empty list if the detected mime type is null, blank, or unknown
     */
    public List<String> getAllowedExtensions(final String detectedMime) {
        if (detectedMime == null || detectedMime.isBlank()) {
            return List.of();
        }

        try {
            final MimeType mimeType = MimeTypes.getDefaultMimeTypes().forName(detectedMime);
            final List<String> exts = mimeType.getExtensions();
            return exts == null ? List.of() : List.copyOf(exts);
        } catch (MimeTypeException e) {
            logger.error("Error retrieving allowed extensions for mime type: " + detectedMime, e);
            return List.of();
        }
    }
}
