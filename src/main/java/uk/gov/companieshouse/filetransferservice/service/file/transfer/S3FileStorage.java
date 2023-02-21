package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Tag;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * An implementation of the FileStorageStrategy for S3
 */
@Component
public class S3FileStorage implements FileStorageStrategy {

    private static final String AV_TIMESTAMP_KEY = "av-timestamp";
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
        amazonFileTransfer.uploadFile(file.getFileName(), new ByteArrayInputStream(file.getBody()));

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
        //todo Can these be done in a single call?
        ObjectMetadata metaData = amazonFileTransfer.getFileMetaData(fileId);
        if (metaData == null) {
            return Optional.empty();
        }

        GetObjectTaggingResult taggingResult = amazonFileTransfer.getFileTaggingResult(fileId);
        if (taggingResult == null) {
            return Optional.empty();
        }

        FileDetailsApi fileDetailsApi = new FileDetailsApi(fileId,
                extractTag(taggingResult, AV_TIMESTAMP_KEY),
                AvStatusApi.valueOf(extractTag(taggingResult, AV_STATUS_KEY).toUpperCase()),
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

    private String extractTag(GetObjectTaggingResult result, String key) {
        Tag tag = result.getTagSet().stream()
                .filter(t -> t.getKey().equals(key))
                .findFirst().orElse(null);

        return tag.getValue();
    }
}
