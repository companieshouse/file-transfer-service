package uk.gov.companieshouse.filetransferservice.service.storage;

import java.util.Optional;
import uk.gov.companieshouse.api.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;

/**
 * An interface exposing the file storage needs of the application.
 * The implementation can be swapped out to change the remote storage type
 */
public interface FileStorageStrategy {

    /**
     * Save a file to a remote repository
     *
     * @param file to upload
     * @return file id used in subsequent calls on the file resource
     */
    String save(FileUploadApi file);

    /**
     * Loads a file stream from a remote repository
     *
     * @param fileDetails file meta data
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    Optional<FileDownloadApi> load(FileDetailsApi fileDetails);

    /**
     * Retrieve a file's details from a remote repository
     *
     * @param fileId of file details to retrieve
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    Optional<FileDetailsApi> getFileDetails(String fileId);

    /**
     * Delete a file with the given file id from a remote repository
     *
     * @param fileId of the file to delete
     */
    void delete(String fileId);

}
