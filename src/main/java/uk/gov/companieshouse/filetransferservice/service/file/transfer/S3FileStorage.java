package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An implementation of the FileStorageStrategy for S3
 */
@Component
public class S3FileStorage implements FileStorageStrategy {

    private static final String AV_TIMESTAMP_KEY = "av-timestamp";
    private static final String AV_STATUS_KEY = "av-status";
    private static final int AV_KEY_COUNT = 2;

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
        S3Object s3Object = amazonFileTransfer.getFileObject(fileId);
        if (s3Object == null) {
            return Optional.empty();
        }

        AvStatusApi avStatus = AvStatusApi.NOT_SCANNED;
        String avCreatedOn = "";

        if (s3Object.getTaggingCount() != null && s3Object.getTaggingCount() > 0) {
            Map<String, String> avTags = extractAVTags(amazonFileTransfer.getFileTags(fileId));

            if (avTags.size() != AV_KEY_COUNT) {
                return Optional.empty();
            }

            avStatus = AvStatusApi.valueOf(avTags.get(AV_STATUS_KEY).toUpperCase());
            avCreatedOn = avTags.get(AV_TIMESTAMP_KEY);
        }

        FileDetailsApi fileDetailsApi = new FileDetailsApi(fileId,
                avCreatedOn,
                avStatus,
                s3Object.getObjectMetadata().getContentType(),
                s3Object.getObjectMetadata().getContentLength(),
                fileId,
                s3Object.getObjectMetadata().getLastModified().toString(),
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

    private Map<String, String> extractAVTags(GetObjectTaggingResult tags) {
        if (tags == null) {
            return new HashMap<>();
        } else {
            return tags.getTagSet().stream()
                    .filter(tag -> AV_TIMESTAMP_KEY.equals(tag.getKey()) || AV_STATUS_KEY.equals(tag.getKey()))
                    .collect((Collectors.toMap(Tag::getKey, Tag::getValue)));
        }
    }
}
