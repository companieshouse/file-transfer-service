package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

/**
 * An implementation of the FileStorageStrategy for S3
 */
@Component
public class S3FileStorage implements FileStorageStrategy {

    private AmazonFileTransfer amazonFileTransfer;

    @Autowired
    public S3FileStorage(AmazonFileTransfer amazonFileTransfer) {
        this.amazonFileTransfer = amazonFileTransfer;
    }

    /**
     * Upload a file to S3
     *
     * @param file to upload
     * @return id used in subsequent calls on the S3 file resource
     */
    @Override
    public String save(FileApi file) {
        amazonFileTransfer.uploadFile(file.getFileName(), new ByteArrayInputStream(file.getBody()), null);

        return file.getFileName();
    }

    /**
     * Download a file from S3
     *
     * @param id          of the file to retrieve
     * @param fileDetails
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileApi> load(String id, FileDetailsApi fileDetails) {
        String contents =amazonFileTransfer.downloadFile("s3://s3av-cidev/2a089d94-785e-42a9-97cf-c85c26ee83ca");

        FileApi fileApi = new FileApi(id, contents.getBytes(), fileDetails.getContentType(), (int)fileDetails.getSize(), null);

        return Optional.of(fileApi);
    }

    /**
     * Retrieve a file's details from S3
     *
     * @param id of the file details to retrieve
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileDetailsApi> getFileDetails(String id) {
        ObjectMetadata metaData =amazonFileTransfer.getFileMetaData("s3://s3av-cidev/0002e92d-d3c8-498c-9c38-b4f1985f4897");
        Map<String, String> tags =amazonFileTransfer.getFileTags("s3://s3av-cidev/0002e92d-d3c8-498c-9c38-b4f1985f4897");

        FileDetailsApi fileDetailsApi = new FileDetailsApi(id, tags.get("av-Timestamp"), AvStatusApi.valueOf(tags.get("av-status").toUpperCase()), metaData.getContentType(), metaData.getContentLength(), id, metaData.getLastModified().toString(), null);

        return Optional.of(fileDetailsApi);
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
