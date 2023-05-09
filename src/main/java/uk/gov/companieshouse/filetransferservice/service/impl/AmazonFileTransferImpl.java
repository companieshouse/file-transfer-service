package uk.gov.companieshouse.filetransferservice.service.impl;

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
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public void uploadFile(String fileId, Map<String, String> metaData, InputStream inputStream) {
        validateS3Details();
        s3Client.putObject(new PutObjectRequest(properties.getBucketName(), fileId, inputStream, createObjectMetaData(metaData)));
    }

    /**
     * Download a file from S3
     *
     * @return String
     */
    @Override
    public Optional<byte[]> downloadFile(String fileId) {
        try {
            validateS3Details();
            // Try-with ensures connections are closed once used.
            try (S3Object s3Object = getObjectInS3(fileId);
                 S3ObjectInputStream is = s3Object.getObjectContent()) {
                return Optional.ofNullable(IOUtils.toByteArray(is));
            }
        } catch (Exception e) {
            logger.errorContext(fileId, "Unable to fetch file from S3", e, new HashMap<>() {{
                put("fileId", fileId);
            }});

            return Optional.empty();
        }
    }

    /**
     * Get file meta data
     *
     * @return String
     */
    @Override
    public Optional<S3Object> getFileObject(String fileId) {
        try {
            validateS3Details();
            return Optional.ofNullable(s3Client.getObject(new GetObjectRequest(properties.getBucketName(), fileId)));
        } catch (Exception e) {
            logger.errorContext(fileId, "Unable to fetch object from S3", e, new HashMap<>() {{
                put("fileId", fileId);
            }});
            return Optional.empty();
        }
    }

    /**
     * Get file meta tags
     *
     * @return Object containing Map of Tags
     */
    @Override
    public Optional<List<Tag>> getFileTags(String fileId) {
        try {
            validateS3Details();
            return Optional.ofNullable(s3Client.getObjectTagging(new GetObjectTaggingRequest(properties.getBucketName(), fileId)).getTagSet());
        } catch (Exception e) {
            logger.errorContext(fileId, "Unable to fetch file tags from S3", e, new HashMap<>() {{
                put("fileId", fileId);
            }});
            return Optional.empty();
        }
    }

    /**
     * Delete an object in S3
     *
     * @throws SdkClientException
     */
    @Override
    public void deleteFile(String fileId) {
        validateS3Details();
        s3Client.deleteObject(new DeleteObjectRequest(properties.getBucketName(), fileId));
    }

    /**
     * Get an object from S3
     *
     * @throws SdkClientException
     */
    private S3Object getObjectInS3(String fileId) {
        if (!s3Client.doesObjectExist(properties.getBucketName(), fileId))
            throw new SdkClientException("S3 path does not exist: " + getObjectPath(fileId));

        return s3Client.getObject(new GetObjectRequest(properties.getBucketName(), fileId));
    }

    private void validateS3Details() {
        if (!validateS3Path())
            throw new SdkClientException("S3 path is invalid");
        if (!validateBucketName())
            throw new SdkClientException("bucket name is invalid");
        if (!s3Client.doesBucketExistV2(properties.getBucketName()))
            throw new SdkClientException("bucket does not exist");
    }

    private ObjectMetadata createObjectMetaData(Map<String, String> metaData) {
        ObjectMetadata omd = new ObjectMetadata();

        if (metaData.containsKey(CONTENT_TYPE)) {
            omd.setContentType(metaData.get(CONTENT_TYPE));
        } else {
            throw new SdkClientException("meta data does not contain Content-Type");
        }

        // Add all other metadata key value pairs to user meta data
        metaData.forEach((k, v) -> {
            if (!k.equalsIgnoreCase(CONTENT_TYPE)) {
                omd.addUserMetadata(k, v);
            }
        });

        return omd;
    }

    private String getS3Path() {
        return String.format("%s%s", properties.getS3PathPrefix(), properties.getBucketName());
    }

    private String getObjectPath(String fileId) {
        return String.format("%s%s/%s", S3_PATH_PREFIX, properties.getBucketName(), fileId);
    }

    private boolean validateS3Path() {
        return getS3Path().toLowerCase().startsWith(S3_PATH_PREFIX);
    }

    private boolean validateBucketName() {
        return !StringUtils.isNullOrEmpty(properties.getBucketName());
    }
}
