package uk.gov.companieshouse.filetransferservice.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.filetransferservice.validation.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.validation.UploadedFileValidator;

import java.io.IOException;
import java.util.Optional;


@Component
public class MultipartFileToFileApiConverter {
    private final UploadedFileValidator uploadedFileValidator;

    @Autowired
    public MultipartFileToFileApiConverter(UploadedFileValidator uploadedFileValidator) {
        this.uploadedFileValidator = uploadedFileValidator;
    }

    @NonNull
    public FileApi convert(@NonNull MultipartFile multipartFile) throws InvalidMimeTypeException, IOException {
        uploadedFileValidator.validate(multipartFile);

        byte[] data = multipartFile.getBytes();
        String fileName = Optional.ofNullable(multipartFile.getOriginalFilename()).orElse("");
        String mimeType = multipartFile.getContentType();
        int size = (int) multipartFile.getSize();
        String extension = getFileExtension(fileName);
        return new FileApi(fileName, data, mimeType, size, extension);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}

