package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

/**
 * An implementation of the FileStorageStrategy for S3
 */
@Component
public class S3FileStorage implements FileStorageStrategy {

    private static final String AV_TIMESTAMP_KEY = "av-Timestamp";
    private static final String AV_STATUS_KEY = "av-status";

    private final AmazonFileTransfer amazonFileTransfer;

    @Autowired
    public S3FileStorage(AmazonFileTransfer amazonFileTransfer) {
        this.amazonFileTransfer = amazonFileTransfer;
    }

    /**
     * Upload a file to S3
     *
     * @param file to upload
     * @return file id used in subsequent calls on the S3 file resource
     */
    @Override
    public String save(FileApi file) {
        amazonFileTransfer.uploadFile(file.getFileName(), new ByteArrayInputStream(file.getBody()), null);

        return file.getFileName();
    }

    /**
     * Download a file from S3
     *
     * @param fileId      of the file to retrieve
     * @param fileDetails file meta data
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileApi> load(String fileId, FileDetailsApi fileDetails) {
        String contents = amazonFileTransfer.downloadFile(fileId);

        if (StringUtils.isNotEmpty(contents)) {
            return Optional.of(new FileApi(
                    fileId,
                    contents.getBytes(),
                    fileDetails.getContentType(),
                    (int) fileDetails.getSize(),
                    null));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Retrieve a file's details from S3
     *
     * @param fileId of the file details to retrieve
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileDetailsApi> getFileDetails(String fileId) {
        ObjectMetadata metaData = amazonFileTransfer.getFileMetaData(fileId);
        if (metaData == null) {
            return Optional.empty();
        }

        Map<String, String> tags = amazonFileTransfer.getFileTags(fileId);
        if (tags == null) {
            return Optional.empty();
        }

        FileDetailsApi fileDetailsApi = new FileDetailsApi(fileId,
                tags.get(AV_TIMESTAMP_KEY),
                AvStatusApi.valueOf(tags.get(AV_STATUS_KEY).toUpperCase()),
                metaData.getContentType(),
                metaData.getContentLength(),
                fileId,
                metaData.getLastModified().toString(),
                null);

        return Optional.of(fileDetailsApi);
    }

    /**
     * Deletes the file from S3
     *
     * @param fileId of the file to delete
     */
    @Override
    public void delete(String fileId) {
        amazonFileTransfer.deleteFile(fileId);
    }
}
