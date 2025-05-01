package uk.gov.companieshouse.filetransferservice.service.storage;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.NOT_SCANNED;
import static uk.gov.companieshouse.api.model.filetransfer.AvStatusApi.valueOf;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.FileLinksApi;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
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

    public S3FileStorage(AmazonFileTransfer amazonFileTransfer, Logger logger) {
        this.amazonFileTransfer = amazonFileTransfer;
        this.logger = logger;
    }

    private static String joinPathSegments(String... strings) {
        return String.join("/", strings).replaceAll("/{2,}", "/");
    }

    /**
     * Upload a file to S3
     *
     * @param file to upload
     * @return file id used in subsequent calls on the S3 file resource
     */
    @Override
    public String save(final FileApi file) {
        Map<String, String> metaData = new HashMap<>();
        metaData.put(CONTENT_TYPE, file.getMimeType());
        metaData.put(FILENAME_METADATA_KEY, file.getFileName());
        metaData.put(EXTENSION_METADATA_KEY, file.getExtension());

        String fileId = UUID.randomUUID().toString();

        amazonFileTransfer.uploadFile(fileId, metaData, new ByteArrayInputStream(file.getBody()));

        return fileId;
    }

    @Override
    public String save(final FileUploadApi file) {
        Map<String, String> metaData = new HashMap<>();
        metaData.put(CONTENT_TYPE, file.getMimeType());
        metaData.put(FILENAME_METADATA_KEY, file.getFileName());
        metaData.put(EXTENSION_METADATA_KEY, file.getExtension());

        String fileId = UUID.randomUUID().toString();

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
    @Deprecated(forRemoval = true)
    public Optional<FileApi> load(final String fileId, final FileDetailsApi fileDetails) {
        return amazonFileTransfer.downloadFile(fileId)
                .map(bytes -> new FileApi(
                        fileDetails.getName(),
                        bytes,
                        fileDetails.getContentType(),
                        bytes.length,
                        null));
    }

    @Override
    public Optional<FileDownloadApi> fetch(final String fileId, final FileDetailsApi fileDetailsApi) {
        Optional<InputStream> inputStream = amazonFileTransfer.downloadStream(fileId);

        return inputStream.map(stream -> new FileDownloadApi(
                fileDetailsApi.getName(),
                stream,
                fileDetailsApi.getContentType(),
                0,
                null));
    }

    /**
     * Retrieve a file's details from S3
     *
     * @param fileId of the file details to retrieve
     * @return Empty, if there is no such file, otherwise the File wrapped in an optional
     */
    @Override
    public Optional<FileDetailsApi> getFileDetails(final String fileId) {
        Optional<ResponseInputStream<GetObjectResponse>> optionalResponse = amazonFileTransfer.getFileObject(fileId);

        if (optionalResponse.isEmpty()) {
            return Optional.empty();
        }

        try (ResponseInputStream<GetObjectResponse> responseInputStream = optionalResponse.get()) {

            GetObjectResponse objectResponse = responseInputStream.response();

            AvStatusApi avStatus = NOT_SCANNED;
            String avCreatedOn = "";

            Integer tagCount = objectResponse.tagCount();
            if (tagCount != null && tagCount > 0) {
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

            Map<String, String> metadata = objectResponse.metadata();
            String fileName = metadata.get(FILENAME_METADATA_KEY);

            FileDetailsApi fileDetailsApi = new FileDetailsApi(fileId,
                    avCreatedOn,
                    avStatus,
                    objectResponse.contentType(),
                    objectResponse.contentLength(),
                    fileName,
                    objectResponse.lastModified().toString(),
                    getLinks(fileId));

            return Optional.of(fileDetailsApi);

        } catch (IOException e) {
            logger.errorContext(fileId, "Error closing S3Object when getting file details", e, null);
            return Optional.empty();
        }
    }

    private FileLinksApi getLinks(final String fileId) {
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
    public void delete(final String fileId) {
        amazonFileTransfer.deleteFile(fileId);
    }

    private Map<String, String> extractAVTags(final List<Tag> tags) {
        return tags.stream()
                .filter(tag -> AV_TIMESTAMP_KEY.equals(tag.key()) || AV_STATUS_KEY.equals(tag.key()))
                .collect((Collectors.toMap(Tag::key, Tag::value)));
    }
}
