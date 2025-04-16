package uk.gov.companieshouse.filetransferservice.service.impl;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.util.StringUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filetransferservice.config.properties.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.model.S3File;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

@Component
public class AmazonFileTransferImpl implements AmazonFileTransfer {

    private static final String S3_PATH_PREFIX = "s3://";

    private final AmazonS3 s3Client;
    private final AWSServiceProperties properties;
    private final Logger logger;

    @Autowired
    public AmazonFileTransferImpl(AmazonS3 s3Client, AWSServiceProperties properties, Logger logger) {
        this.s3Client = s3Client;
        this.logger = logger;
        this.properties = properties;
    }

    /**
     * Upload the file to S3
     */
    @Override
    public void uploadFile(final String fileId, final Map<String, String> metaData, final InputStream inputStream) {
        logger.trace(format("uploadFile(fileId=%s, metaData=%s) method called.", fileId, metaData));

        validateS3Details();

        try {
            logger.debug(format("Uploading file '%s' to '%s'...", fileId, S3_PATH_PREFIX));

            ObjectMetadata objectMetadata = createObjectMetaData(metaData);
            PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), fileId, inputStream, objectMetadata);

            s3Client.putObject(putObjectRequest);

        } catch(SdkClientException ex) {
            logger.error("An exception occurred writing to bucket: ", ex);
        }
    }

    /**
     * Download a file from S3
     */
    @Override
    public Optional<byte[]> downloadFile(final String fileId) {
        logger.trace(format("downloadFile(fileId=%s) method called.", fileId));

        validateS3Details();

        try (S3Object s3Object = getObjectInS3(fileId);
            S3ObjectInputStream is = s3Object.getObjectContent()) {

            logger.debug(format("Downloading file %s from %s...", fileId, S3_PATH_PREFIX));
            byte[] readData = readBytesFromStream(is);

            logger.debug(format("The size of the file downloaded from S3 is: %d", readData.length));
            return Optional.of(readData);

        } catch(SdkClientException | IOException e) {
            logger.errorContext(fileId, "Unable to fetch file from S3", e, new HashMap<>() {{
                put("fileId", fileId);
            }});

            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> downloadStream(final String fileId) {
        logger.trace(format("downloadStream(fileId=%s) method called.", fileId));

        validateS3Details();

        return getFileObject(fileId).map(S3Object::getObjectContent);
    }

    private byte[] readBytesFromStream(final S3ObjectInputStream input) throws IOException {
        logger.debug(format("readBytesFromStream(%d bytes available) method called.", input.available()));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        input.transferTo(output);

        return output.toByteArray();
    }

    /**
     * Get file metadata
     */
    @Override
    public Optional<S3Object> getFileObject(final String fileId) {
        logger.debug(format("getFileObject(fileId=%s) method called.", fileId));

        validateS3Details();

        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(properties.getBucketName(), fileId);
            S3Object object = s3Client.getObject(getObjectRequest);

            return Optional.ofNullable(object);

        } catch (SdkClientException e) {
            logger.errorContext(fileId, "Unable to fetch object from S3", e, new HashMap<>() {{
                put("fileId", fileId);
            }});
            return Optional.empty();
        }
    }

    /**
     * Get file meta tags
     */
    @Override
    public Optional<List<Tag>> getFileTags(final String fileId) {
        logger.debug(format("getFileTags(fileId=%s) method called.", fileId));

        validateS3Details();

        try {
            GetObjectTaggingRequest getObjectTaggingRequest = new GetObjectTaggingRequest(properties.getBucketName(), fileId);
            List<Tag> tagSet = s3Client.getObjectTagging(getObjectTaggingRequest).getTagSet();

            return Optional.ofNullable(tagSet);

        } catch (SdkClientException e) {
            logger.errorContext(fileId, "Unable to fetch file tags from S3", e, new HashMap<>() {{
                put("fileId", fileId);
            }});
            return Optional.empty();
        }
    }

    /**
     * Delete an object in S3
     */
    @Override
    public void deleteFile(final String fileId) {
        logger.trace(format("deleteFile(fileId=%s) method called.", fileId));

        validateS3Details();

        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(properties.getBucketName(), fileId);
        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * Get an object from S3
     */
    private S3Object getObjectInS3(final String fileId) {
        logger.trace(format("getObjectInS3(%s) method called.", fileId));

        if (!s3Client.doesObjectExist(properties.getBucketName(), fileId))
            throw new SdkClientException("S3 Path does not exist: " + getObjectPath(fileId));

        GetObjectRequest getObjectRequest = new GetObjectRequest(properties.getBucketName(), fileId);
        return s3Client.getObject(getObjectRequest);
    }

    private void validateS3Details() {
        logger.trace("validateS3Details() method called.");

        if (!validateS3Path())
            throw new SdkClientException(format("S3 Path is invalid: [%s]", getS3Path()));

        if (!validateBucketName())
            throw new SdkClientException(format("S3 Bucket Name is invalid: [%s]", properties.getBucketName()));

        if (!s3Client.doesBucketExistV2(properties.getBucketName()))
            throw new SdkClientException(format("S3 Bucket does not exist: [%s]", properties.getBucketName()));
    }

    private ObjectMetadata createObjectMetaData(final Map<String, String> metaData) {
        ObjectMetadata omd = new ObjectMetadata();

        if (metaData.containsKey(CONTENT_TYPE)) {
            omd.setContentType(metaData.get(CONTENT_TYPE));
        } else {
            throw new SdkClientException("meta data does not contain Content-Type");
        }

        // Add all other metadata key value pairs to user metadata
        metaData.forEach((k, v) -> {
            if (!k.equalsIgnoreCase(CONTENT_TYPE)) {
                omd.addUserMetadata(k, v);
            }
        });

        return omd;
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
        return !StringUtils.isNullOrEmpty(properties.getBucketName());
    }
}
