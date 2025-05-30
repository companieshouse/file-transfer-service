package uk.gov.companieshouse.filetransferservice.service.impl;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.BufferedInputStream;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.companieshouse.filetransferservice.config.properties.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

@Component
public class AmazonFileTransferImpl implements AmazonFileTransfer {

    private final String s3PathPrefix;
    private final S3Client s3Client;
    private final AWSServiceProperties properties;
    private final Logger logger;

    public AmazonFileTransferImpl(S3Client s3Client, AWSServiceProperties properties, Logger logger) {
        this.s3Client = s3Client;
        this.logger = logger;
        this.properties = properties;
        this.s3PathPrefix = properties.getS3PathPrefix();

        validateS3Details();
    }

    /**
     * Upload the file to S3
     */
    @Override
    public void uploadFile(final String fileId, final Map<String, String> metadata, final InputStream inputStream) {
        logger.trace(format("uploadFile(fileId=%s, metaData=%s) method called.", fileId, metadata));

        try {
            logger.debug(format("Uploading file '%s' to '%s'...", fileId, s3PathPrefix));

            if (!metadata.containsKey(CONTENT_TYPE)) {
                logger.error("Missing content-type");
                throw SdkClientException.create("metadata does not contain Content-Type");
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(fileId)
                    .metadata(metadata)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));

        } catch (IOException ex) {
            logger.error("An IOException occurred writing to bucket", ex);
            throw SdkClientException.create("An IOException occurred writing to bucket", ex);
        }
    }

    @Override
    public Optional<InputStream> downloadStream(final String fileId) {
        logger.trace(format("downloadStream(fileId=%s) method called.", fileId));

        return getFileObject(fileId).map(BufferedInputStream::new);
    }

    /**
     * Get an object from S3
     */
    @Override
    public Optional<ResponseInputStream<GetObjectResponse>> getFileObject(final String fileId) {
        logger.trace(format("getFileObject(fileId=%s) method called.", fileId));

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(fileId)
                    .build();

            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);

            return Optional.ofNullable(responseInputStream);

        } catch(NoSuchKeyException ex) {
            logger.errorContext(fileId, "Unable to fetch object from S3", ex, loggedFileIdMap(fileId));
            return Optional.empty();
        }
    }

    /**
     * Get file meta tags
     */
    @Override
    public Optional<List<Tag>> getFileTags(final String fileId) {
        logger.trace(format("getFileTags(fileId=%s) method called.", fileId));

        try {
            GetObjectTaggingRequest getObjectTaggingRequest = GetObjectTaggingRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(fileId)
                    .build();

            List<Tag> tagSet = s3Client.getObjectTagging(getObjectTaggingRequest).tagSet();

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

    private boolean checkBucketExists(final String bucket) {
        logger.trace(format("checkBucketExists(bucket=%s) method called.", bucket));

        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();

            s3Client.headBucket(headBucketRequest);

            logger.trace(format("Bucket exists. [%s]", bucket));

            return true;

        } catch (Exception e) {
            logger.errorContext("Unable to verify that S3 bucket exists", e, loggedFileIdMap(bucket));
            return false;
        }
    }

    private String getS3Path() {
        return format("%s%s", properties.getS3PathPrefix(), properties.getBucketName());
    }

    private boolean validateS3Path() {
        return getS3Path().toLowerCase().startsWith(s3PathPrefix);
    }

    private boolean validateBucketName() {
        return !StringUtils.isBlank(properties.getBucketName());
    }

    private static Map<String, Object> loggedFileIdMap(final String fileId) {
        Map<String, Object> map = new HashMap<>();
        map.put("fileId", fileId);
        return map;
    }
}
