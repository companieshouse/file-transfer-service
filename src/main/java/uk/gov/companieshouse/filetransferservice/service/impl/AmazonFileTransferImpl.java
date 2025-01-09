package uk.gov.companieshouse.filetransferservice.service.impl;

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
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

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
                //FIXME
                if(is!=null&&is.readAllBytes()!=null)
         logger.debug("line is "+is.readAllBytes().toString());
                //FIXME
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
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(properties.getBucketName(), fileId));
            //FIXME
            if (s3Object != null) {
                logger.debug(s3Object.toString());

            }
            if (s3Object != null && s3Object.getObjectMetadata() != null) {
                logger.debug("1111" + s3Object.getObjectMetadata().getETag());
                String hh = s3Object.getObjectMetadata().getETag();
                int fg = 0;
            }
            if (s3Object != null && s3Object.getObjectContent() != null) {
                logger.debug("1113" + s3Object.getObjectContent().toString());
                String hh = s3Object.getObjectContent().toString();
                int df = 0;
            }
//FIXME


            S3ObjectInputStream s3ObjectInputStream = null;
            ByteArrayOutputStream remainingDataStream = new ByteArrayOutputStream();

            try {
                // Retrieve the object content (the InputStream)
                s3ObjectInputStream = s3Object.getObjectContent();

                // Read from the stream and convert to a human-readable String
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append(System.lineSeparator()); // Add a newline after each line
                        // Log each line of the content
                        logger.debug("Read line: {}+ line");
                    }

                    // Now the content contains the object as a human-readable String
                    logger.debug("Object Content:{}" + content.toString());
                } catch (IOException e) {
                    logger.error("IOException occurred while reading the object content.", e);
                }

            } finally {
                // Ensure the remaining content in the stream is captured and printed
                if (s3ObjectInputStream != null) {
                    try {
                        // Before closing the stream, capture any remaining data
                        byte[] remainingBytes = new byte[8921];
                        int bytesRead;
                        while ((bytesRead = s3ObjectInputStream.read(remainingBytes)) != -1) {
                            remainingDataStream.write(remainingBytes, 0, bytesRead);
                        }

                        // Print the remaining data in the stream
                        if (remainingDataStream.size() > 0) {
                            String remainingContent = new String(remainingDataStream.toByteArray(), StandardCharsets.UTF_8);
                            logger.debug("Remaining Data in Stream:\n{}" + remainingContent);
                        }

                        // Close the stream after capturing remaining data
                        s3ObjectInputStream.close();
                        logger.debug("Stream closed successfully.");

                    } catch (IOException e) {
                        logger.error("IOException occurred while closing the stream.", e);
                    }
                }

                // Any other necessary cleanup can be added here
                logger.debug("Finally block executed.");
                return Optional.of(s3Object);

            }


        }
    catch (Exception e) {
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
            S3Object s3Object=s3Client.getObject(new GetObjectRequest(properties.getBucketName(), fileId));
            if(s3Object!=null&&s3Object.getObjectMetadata()!=null)
            logger.debug(s3Object.getObjectMetadata().getETag());
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
