package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.NOT_SCANNED;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.valueOf;

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
        String fileId = UUID.randomUUID().toString();
        Map<String, String> metaData = new HashMap<>();
        metaData.put(CONTENT_TYPE, file.getMimeType());

        amazonFileTransfer.uploadFile(fileId, metaData, new ByteArrayInputStream(file.getBody()));

        return fileId;
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
        return amazonFileTransfer
                .downloadFile(fileId)
                .map(bytes -> new FileApi(
                        fileId,
                        bytes,
                        fileDetails.getContentType(),
                        bytes.length,
                        null));
    }

    /**
     * Retrieve a file's details from S3
     *
     * @param fileId of the file details to retrieve
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileDetailsApi> getFileDetails(String fileId) {
        Optional<S3Object> s3Object = amazonFileTransfer.getFileObject(fileId);

        if (s3Object.isEmpty()) {
            return Optional.empty();
        }

        AvStatusApi avStatus = NOT_SCANNED;
        String avCreatedOn = "";

        S3Object s3File = s3Object.get();

        if (s3File.getTaggingCount() != null && s3File.getTaggingCount() > 0) {
            Optional<List<Tag>> allTags = amazonFileTransfer.getFileTags(fileId);
            if (allTags.isEmpty()) {
                return Optional.empty();
            }

            Map<String, String> avTags = extractAVTags(allTags.get());
            if (avTags.size() != AV_KEY_COUNT) {
                return Optional.empty();
            }

            avStatus = valueOf(avTags.get(AV_STATUS_KEY).toUpperCase());
            avCreatedOn = avTags.get(AV_TIMESTAMP_KEY);
        }

        FileDetailsApi fileDetailsApi = new FileDetailsApi(fileId,
                avCreatedOn,
                avStatus,
                s3File.getObjectMetadata().getContentType(),
                s3File.getObjectMetadata().getContentLength(),
                fileId,
                s3File.getObjectMetadata().getLastModified().toString(),
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

    private Map<String, String> extractAVTags(List<Tag> tags) {
        return tags.stream()
                .filter(tag -> AV_TIMESTAMP_KEY.equals(tag.getKey()) || AV_STATUS_KEY.equals(tag.getKey()))
                .collect((Collectors.toMap(Tag::getKey, Tag::getValue)));
    }
}
