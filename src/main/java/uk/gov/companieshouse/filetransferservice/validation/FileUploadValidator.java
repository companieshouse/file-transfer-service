package uk.gov.companieshouse.filetransferservice.validation;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.logging.Logger;

@Component
public class FileUploadValidator {

    private final Logger logger;

    @Autowired
    public FileUploadValidator(final Logger logger){
        this.logger = logger;
    }

    public void validate(final MultipartFile file) throws IOException {
        logger.trace(String.format("Validating file: %s", file.getOriginalFilename()));

        if (requireNonNull(file.getOriginalFilename()).isEmpty()) {
            throw new IOException(String.format("Uploaded file has no filename: %s", file.getOriginalFilename()));
        }

        file.getInputStream().close();
    }
}
