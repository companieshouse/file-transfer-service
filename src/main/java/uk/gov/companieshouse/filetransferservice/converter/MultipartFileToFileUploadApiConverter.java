package uk.gov.companieshouse.filetransferservice.converter;

import static java.util.Objects.requireNonNullElseGet;

import java.io.IOException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;

@Component
public class MultipartFileToFileUploadApiConverter implements Converter<MultipartFile, FileUploadApi> {

    @Override
    public FileUploadApi convert(final MultipartFile source) {
        String originalFilename = requireNonNullElseGet(source.getOriginalFilename(), () -> "No filename provided");

        FileUploadApi fileUploadApi = new FileUploadApi();
        fileUploadApi.setFileName(originalFilename);
        try {
            fileUploadApi.setBody(source.getInputStream());
            fileUploadApi.setSize(source.getInputStream().readAllBytes().length);

        } catch(IOException ex) {
            fileUploadApi.setBody(null);
            fileUploadApi.setSize(0);
        }
        fileUploadApi.setMimeType(source.getContentType());
        fileUploadApi.setExtension(originalFilename.substring(originalFilename.lastIndexOf(".")));

        return fileUploadApi;
    }

}
