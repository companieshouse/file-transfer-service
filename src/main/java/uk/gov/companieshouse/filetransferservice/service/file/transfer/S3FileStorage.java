package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.NOT_SCANNED;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.valueOf;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.FileLinksApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An implementation of the FileStorageStrategy for S3
 */
@Component
public class S3FileStorage implements FileStorageStrategy {
    public static final String FILENAME_METADATA_KEY = "filename";

    private static final String AV_TIMESTAMP_KEY = "av-timestamp";
    private static final String AV_STATUS_KEY = "av-status";
    private static final int AV_KEY_COUNT = 2;
    public static final String EXTENSION_METADATA_KEY = "extension";
    private final Logger logger;

    private final AmazonFileTransfer amazonFileTransfer;
    @Value("${service.path.prefix}")
    private String servicePathPrefix;

    @Autowired
    public S3FileStorage(AmazonFileTransfer amazonFileTransfer, Logger logger) {
        this.amazonFileTransfer = amazonFileTransfer;
        this.logger = logger;
    }

    public static String joinPathSegments(String... strings) {
        return String.join("/", strings).replaceAll("/{2,}", "/");
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
        metaData.put(FILENAME_METADATA_KEY, file.getFileName());
        metaData.put(EXTENSION_METADATA_KEY, file.getExtension());

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
                        fileDetails.getName(),
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
        Optional<S3Object> optionalS3Object = amazonFileTransfer.getFileObject(fileId);
        if (optionalS3Object.isEmpty()) {
            return Optional.empty();
        }

        logger.debug(optionalS3Object.toString());


        FileDetailsApi fileDetailsApi = null;
        try (S3Object s3Object = optionalS3Object.get()) {

            AvStatusApi avStatus = NOT_SCANNED;
            String avCreatedOn = "";

            if (s3Object.getTaggingCount() != null && s3Object.getTaggingCount() > 0) {
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

            String fileName = s3Object.getObjectMetadata().getUserMetaDataOf(FILENAME_METADATA_KEY);

            fileDetailsApi = new FileDetailsApi(fileId,
                    avCreatedOn,
                    avStatus,
                    s3Object.getObjectMetadata().getContentType(),
                    s3Object.getObjectMetadata().getContentLength(),
                    fileName,
                    s3Object.getObjectMetadata().getLastModified().toString(),
                    getLinks(fileId));

            return Optional.of(fileDetailsApi);
        } catch (IOException e) {
            logger.errorContext(fileId, "Error closing S3Object when getting file details", e, null);
            return Optional.ofNullable(fileDetailsApi);
        }
    }

    private FileLinksApi getLinks(String fileId) {
        String selfLink = joinPathSegments(servicePathPrefix, fileId);
        String downloadLink = joinPathSegments(selfLink, "download");
        return new FileLinksApi(downloadLink, selfLink);
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
