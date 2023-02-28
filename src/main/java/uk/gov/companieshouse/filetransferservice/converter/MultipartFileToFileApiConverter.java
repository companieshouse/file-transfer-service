package uk.gov.companieshouse.filetransferservice.converter;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;

import java.io.IOException;
import java.util.Optional;


@Component
public class MultipartFileToFileApiConverter {

    @NonNull
    public FileApi convert(@NonNull MultipartFile multipartFile) throws IOException {
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

