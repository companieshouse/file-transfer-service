package uk.gov.companieshouse.filetransferservice.service.impl;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.companieshouse.filetransferservice.config.properties.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

@Component
public class AmazonFileTransferImpl implements AmazonFileTransfer {

    private static final String S3_PATH_PREFIX = "s3://";

    private final S3Client s3Client;
    private final AWSServiceProperties properties;
    private final Logger logger;

    public AmazonFileTransferImpl(S3Client s3Client, AWSServiceProperties properties, Logger logger) {
        this.s3Client = s3Client;
        this.logger = logger;
        this.properties = properties;

        validateS3Details();
    }

    /**
     * Upload the file to S3
     */
    @Override
    public void uploadFile(final String fileId, final Map<String, String> metadata, final InputStream inputStream) {
        logger.trace(format("uploadFile(fileId=%s, metaData=%s) method called.", fileId, metadata));

        try {
            logger.debug(format("Uploading file '%s' to '%s'...", fileId, S3_PATH_PREFIX));

            if (!metadata.containsKey(CONTENT_TYPE)) {
                throw SdkClientException.create("metadata does not contain Content-Type");
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(fileId)
                    .metadata(metadata)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));

        } catch (SdkClientException ex) {
            logger.error("An SdkClientException occurred writing to bucket", ex);
            throw ex;
        } catch (IOException ex) {
            logger.error("An IOException occurred writing to bucket", ex);
            throw SdkClientException.create("An IOException occurred writing to bucket", ex);
        }
    }

    /**
     * Download a file from S3
     */
    @Override
    @Deprecated(forRemoval = true)
    public Optional<byte[]> downloadFile(final String fileId) {
        logger.trace(format("downloadFile(fileId=%s) method called.", fileId));

        try (ResponseInputStream<GetObjectResponse> responseInputStream = getObjectInS3(fileId)) {

            logger.debug(format("Downloading file %s from %s...", fileId, S3_PATH_PREFIX));
            byte[] readData = readBytesFromStream(responseInputStream);

            logger.debug(format("The size of the file downloaded from S3 is: %d", readData.length));
            return Optional.of(readData);

        } catch (SdkClientException | IOException e) {
            logger.errorContext(fileId, "Unable to fetch file from S3", e, loggedFileIdMap(fileId));

            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> downloadStream(final String fileId) {
        logger.trace(format("downloadStream(fileId=%s) method called.", fileId));

        return getFileObject(fileId)
                .map(BufferedInputStream::new);
    }

    private byte[] readBytesFromStream(final InputStream input) throws IOException {
        logger.debug(format("readBytesFromStream(%d bytes available) method called.", input.available()));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        input.transferTo(output);

        return output.toByteArray();
    }

    /**
     * Get file metadata
     */
    @Override
    public Optional<ResponseInputStream<GetObjectResponse>> getFileObject(final String fileId) {
        logger.debug(format("getFileObject(fileId=%s) method called.", fileId));

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(fileId)
                    .build();

            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);

            return Optional.ofNullable(responseInputStream);

        } catch (SdkClientException e) {
            logger.errorContext(fileId, "Unable to fetch object from S3", e, loggedFileIdMap(fileId));
            return Optional.empty();
        }
    }

    /**
     * Get file meta tags
     */
    @Override
    public Optional<List<Tag>> getFileTags(final String fileId) {
        logger.debug(format("getFileTags(fileId=%s) method called.", fileId));

        try {
            GetObjectTaggingRequest getObjectTaggingRequest = GetObjectTaggingRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(fileId)
                    .build();
            List<Tag> tagSet = s3Client.getObjectTagging(getObjectTaggingRequest)
                    .tagSet();

            return Optional.ofNullable(tagSet);

        } catch (SdkClientException e) {
            logger.errorContext(fileId, "Unable to fetch file tags from S3", e, loggedFileIdMap(fileId));
            return Optional.empty();
        }
    }

    /**
     * Delete an object in S3
     */
    @Override
    public void deleteFile(final String fileId) {
        logger.trace(format("deleteFile(fileId=%s) method called.", fileId));

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(fileId)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * Get an object from S3
     */
    private ResponseInputStream<GetObjectResponse> getObjectInS3(final String fileId) {
        logger.trace(format("getObjectInS3(%s) method called.", fileId));

        if (!checkObjectExists(properties.getBucketName())) {
            throw SdkClientException.create("S3 Path does not exist: " + getObjectPath(fileId));
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(fileId)
                .build();
        return s3Client.getObject(getObjectRequest);
    }

    private void validateS3Details() {
        logger.trace("validateS3Details() method called.");

        if (!validateS3Path()) {
            throw SdkClientException.create(format("S3 Path is invalid: [%s]", getS3Path()));
        }

        if (!validateBucketName()) {
            throw SdkClientException.create(format("S3 Bucket Name is invalid: [%s]", properties.getBucketName()));
        }

        if (!checkBucketExists(properties.getBucketName())) {
            throw SdkClientException.create(format("S3 Bucket does not exist: [%s]", properties.getBucketName()));
        }
    }

    private String getS3Path() {
        return format("%s%s", properties.getS3PathPrefix(), properties.getBucketName());
    }

    private String getObjectPath(String fileId) {
        return format("%s%s/%s", S3_PATH_PREFIX, properties.getBucketName(), fileId);
    }

    private boolean validateS3Path() {
        return getS3Path().toLowerCase().startsWith(S3_PATH_PREFIX);
    }

    private boolean validateBucketName() {
        return !StringUtils.isBlank(properties.getBucketName());
    }

    private boolean checkObjectExists(String fileId) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(fileId)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.sdkHttpResponse().isSuccessful();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            } else {
                throw SdkClientException.create("S3 Path does not exist: " + getObjectPath(fileId), e);
            }
        }
    }

    private boolean checkBucketExists(String bucket) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucket)
                .build();

        try {
            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    private static Map<String, Object> loggedFileIdMap(String fileId) {
        Map<String, Object> map = new HashMap<>();
        map.put("fileId", fileId);
        return map;
    }
}
