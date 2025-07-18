package uk.gov.companieshouse.filetransferservice.service.storage;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;
import uk.gov.companieshouse.api.filetransfer.AvStatus;
import uk.gov.companieshouse.api.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.filetransfer.FileLinksApi;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.filetransferservice.service.converter.MetadataDecoder;
import uk.gov.companieshouse.filetransferservice.service.converter.MetadataEncoder;
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
    private static final String EXTENSION_METADATA_KEY = "extension";

    private final AmazonFileTransfer amazonFileTransfer;
    private final MetadataEncoder metadataEncoder;
    private final MetadataDecoder metadataDecoder;
    private final Logger logger;
    private final String servicePathPrefix;

    public S3FileStorage(AmazonFileTransfer amazonFileTransfer,
            MetadataEncoder metadataEncoder,
            MetadataDecoder metadataDecoder,
            Logger logger,
            @Value("${service.path.prefix}") String servicePathPrefix) {
        this.amazonFileTransfer = amazonFileTransfer;
        this.metadataEncoder = metadataEncoder;
        this.metadataDecoder = metadataDecoder;
        this.logger = logger;
        this.servicePathPrefix = servicePathPrefix;
    }

    private static String joinPathSegments(String... strings) {
        return String.join("/", strings).replaceAll("/{2,}", "/");
    }

    @Override
    public String save(final FileUploadApi file) {
        Map<String, String> metaData = new HashMap<>();
        metaData.put(CONTENT_TYPE, file.getMimeType());
        metaData.put(FILENAME_METADATA_KEY, metadataEncoder.convert(file.getFileName()));
        metaData.put(EXTENSION_METADATA_KEY, file.getExtension());

        String fileId = UUID.randomUUID().toString();

        amazonFileTransfer.uploadFile(fileId, metaData, file.getBody());

        return fileId;
    }

    @Override
    public Optional<FileDownloadApi> load(final FileDetailsApi fileDetailsApi) {
        Optional<InputStream> inputStream = amazonFileTransfer.downloadStream(fileDetailsApi.getId());

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

            AvStatus avStatus = AvStatus.NOT_SCANNED;
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

                avStatus = AvStatus.valueOf(avTags.get(AV_STATUS_KEY).toUpperCase());
                avCreatedOn = avTags.get(AV_TIMESTAMP_KEY);
            }

            // Decode metadata values as S3 restricts the values that can be stored as metadata.
            Map<String, String> decodedMap = objectResponse.metadata()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Entry::getKey,entry -> metadataDecoder.convert(entry.getValue())
                    ));

            // Ensure metadata is case insensitive
            Map<String, String> metadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            //metadata.putAll(decodedMap);
            metadata.putAll(objectResponse.metadata());

            logger.info(format("Retrieved file metadata from S3: %s", metadata));

            FileDetailsApi fileDetailsApi = new FileDetailsApi(fileId,
                    avCreatedOn,
                    avStatus,
                    metadata.get(CONTENT_TYPE),
                    objectResponse.contentLength(),
                    metadata.get(FILENAME_METADATA_KEY),
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
