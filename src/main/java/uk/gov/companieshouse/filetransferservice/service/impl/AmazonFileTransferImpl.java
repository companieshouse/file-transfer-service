package uk.gov.companieshouse.filetransferservice.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Validate;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.model.S3FileMetadata;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

@Component
public class AmazonFileTransferImpl implements AmazonFileTransfer {

    private static final String S3_PATH_PREFIX = "s3://";

    private final S3Client s3Client;
    private final AWSServiceProperties properties;
    private final Logger logger;


    @Autowired
    public AmazonFileTransferImpl(S3Client s3Client, AWSServiceProperties properties, Logger logger) {
        this.s3Client = s3Client;
        this.logger = logger;
        this.properties = properties;
    }

    /**
     * Upload the file to S3
     */
    @Override
    public void uploadFile(String fileId, Map<String, String> metaData, byte[] data) {

        validateS3Details();

        PutObjectRequest request = PutObjectRequest.builder()
                                                    .bucket(properties.getBucketName())
                                                    .key(fileId)
                                                    .metadata(metaData)
                                                    .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
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
            try (ResponseInputStream<GetObjectResponse> s3Object = getObjectInS3(fileId);) {

                byte[] readData = readBytesFromStream(s3Object);
                logger.info(String.format("The size of the file downloaded from S3 is: %d", readData.length));
                return Optional.ofNullable(readData);
            }
        } catch (Exception e) {
            logger.errorContext(fileId, "Unable to fetch file from S3", e, new HashMap<>() {{
                put("fileId", fileId);
            }});

            return Optional.empty();
        }
    }

    @Override
    public OutputStream downloadFileAStream(String fileId) {
        OutputStream outputStream = new ByteArrayOutputStream();
        ResponseInputStream<GetObjectResponse> s3Object = getObjectInS3(fileId);
        try {
            s3Object.transferTo(outputStream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return outputStream;
    }

    private byte[] readBytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;

        while((bytesRead = is.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        byte[] readData = buffer.toByteArray();
        return readData;
    }

    /**
     * Get file meta data
     *
     * @return String
     */
    @Override
    public Optional<S3FileMetadata> getFileMetadata(String fileId) {
        try {
            validateS3Details();

        GetObjectTaggingRequest getObjectRequest = GetObjectTaggingRequest.builder()
            .bucket(properties.getBucketName())
            .key(fileId)
            .build();

        HeadObjectRequest metadataRequest = HeadObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(fileId)
                .build();

            HeadObjectResponse metadataResponse = s3Client.headObject(metadataRequest);

            S3FileMetadata fileMetadata = new S3FileMetadata(s3Client.getObjectTagging(getObjectRequest).tagSet(), metadataResponse );
            return Optional.of(fileMetadata);
        } catch (Exception e) {
            logger.errorContext(fileId, "Unable to fetch object from S3", e, new HashMap<>() {{
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

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                                            .bucket(properties.getBucketName())
                                            .key(fileId)
                                            .build();

        s3Client.deleteObject(request);
    }

    /**
     * Get an object from S3
     *
     * @throws SdkClientException
     */
    private ResponseInputStream<GetObjectResponse> getObjectInS3(String fileId) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(fileId)
                .build();

        return s3Client.getObject(request);
    }

    private void validateS3Details() {
        if (!validateS3Path())
            throw SdkClientException.builder().message("S3 path is invalid").build();
        if (!validateBucketName())
            throw SdkClientException.builder().message("bucket name is invalid").build();
        if (!doesBucketExist(properties.getBucketName()))
            throw SdkClientException.builder().message("bucket does not exist").build();
    }

    private String getS3Path() {
        return String.format("%s%s", properties.getS3PathPrefix(), properties.getBucketName());
    }

    private boolean validateS3Path() {
        return getS3Path().toLowerCase().startsWith(S3_PATH_PREFIX);
    }

    private boolean validateBucketName() {
        return ! properties.getBucketName().isBlank();
    }

    private boolean doesBucketExist(String bucketName) {
        try {
            s3Client.getBucketAcl(r -> r.bucket(bucketName));
            return true;
        } catch (AwsServiceException ase) {
            // A redirect error or an AccessDenied exception means the bucket exists but it's not in this region
            // or we don't have permissions to it.
            if ((ase.statusCode() == HttpStatusCode.MOVED_PERMANENTLY) || "AccessDenied".equals(ase.awsErrorDetails().errorCode())) {
                return true;
            }
            if (ase.statusCode() == HttpStatusCode.NOT_FOUND) {
                return false;
            }
            throw ase;
        }
    }
}
