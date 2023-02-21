package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;

import java.util.Optional;

/**
 * An implementation of the FileStorageStrategy for S3
 */
@Component
public class S3FileStorage implements FileStorageStrategy {
    /**
     * Upload a file to S3
     *
     * @param file to upload
     * @return id used in subsequent calls on the S3 file resource
     */
    @Override
    public String save(FileApi file) {
        return "";
    }

    /**
     * Download a file from S3
     *
     * @param id of the file to retrieve
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileApi> load(String id) {
        return Optional.empty();
    }

    /**
     * Retrieve a file's details from S3
     *
     * @param id of the file details to retrieve
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileDetailsApi> getFileDetails(String id) {
        return Optional.empty();
    }

    /**
     * Deletes the file from S3
     *
     * @param id of the file to delete
     */
    @Override
    public void delete(String id) {
    }
}
