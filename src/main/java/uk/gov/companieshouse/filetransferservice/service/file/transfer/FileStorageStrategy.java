package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;

import java.util.Optional;

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
    String save(FileApi file);

    /**
     * Loads a file from a remote repository
     *
     * @param fileId      of the file to retrieve
     * @param fileDetails file meta data
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    Optional<FileApi> load(String fileId, FileDetailsApi fileDetails);

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
