package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.NOT_SCANNED;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.valueOf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.model.Tag;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.FileLinksApi;
import uk.gov.companieshouse.filetransferservice.model.S3FileMetadata;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

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

        amazonFileTransfer.uploadFile(fileId, metaData, file.getBody());

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
        Optional<S3FileMetadata> potentialFileMetadata = amazonFileTransfer.getFileMetadata(fileId);

        if (!potentialFileMetadata.isPresent() || potentialFileMetadata.get().getTags().isEmpty()) {
            return Optional.empty();
        }

        FileDetailsApi fileDetailsApi = null;

        AvStatusApi avStatus = NOT_SCANNED;
        String avCreatedOn = "";

        S3FileMetadata fileMetadata = potentialFileMetadata.get();

        Optional<List<Tag>> allTags = Optional.ofNullable(potentialFileMetadata.get().getTags());
        if (allTags.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> avTags = extractAVTags(allTags.get());
        if (avTags.size() != AV_KEY_COUNT) {
            return Optional.empty();
        }

        avStatus = valueOf(avTags.get(AV_STATUS_KEY).toUpperCase());
        avCreatedOn = avTags.get(AV_TIMESTAMP_KEY);


        fileDetailsApi = new FileDetailsApi(fileId,
                avCreatedOn,
                avStatus,
                fileMetadata.getMetadata().contentType(),
                fileMetadata.getMetadata().contentLength(),
                fileId,
                fileMetadata.getMetadata().lastModified().toString(),
                getLinks(fileId));

        return Optional.of(fileDetailsApi);
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
                .filter(tag -> AV_TIMESTAMP_KEY.equals(tag.key()) || AV_STATUS_KEY.equals(tag.key()))
                .collect((Collectors.toMap(Tag::key, Tag::value)));
    }
}
