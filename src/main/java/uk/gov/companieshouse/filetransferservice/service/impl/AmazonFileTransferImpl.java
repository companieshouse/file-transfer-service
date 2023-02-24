package uk.gov.companieshouse.filetransferservice.service.impl;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;

@Component
public class AmazonFileTransferImpl implements AmazonFileTransfer {

    private static final String SUFFIX = "/";
    private static final String S3_PATH_PREFIX = "s3://";
    private static final String FOLDER_ERROR_MESSAGE = "Subfolder path does not exist";
    private static final String PROXY_HOST_PROPERTY = "IMAGE_CLOUD_PROXY_HOST";
    private static final String PROXY_PORT_PROPERTY = "IMAGE_CLOUD_PROXY_PORT";

    private static final int SPLIT_S3_PATH_SUBSTRING = 5;
    private static final int GET_S3_PATH_FROM_SPLIT = 2;

    private static final Logger LOG = LoggerFactory.getLogger("file-transfer-service");

    private final AWSServiceProperties configuration;

    @Autowired
    public AmazonFileTransferImpl(AWSServiceProperties configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the AWS credentials
     */
    @Override
    public BasicAWSCredentials getAWSCredentials() {
        return new BasicAWSCredentials(configuration.getAccessKeyId(), configuration.getSecretAccessKey());
    }

    /**
     * Upload the file to S3
     */
    @Override
    public void uploadFile(String fileId, InputStream inputStream) {
        AmazonS3 s3Client = getAmazonS3Client();
        validateS3Details(s3Client);
        if (validatePathExists(s3Client)) {
            s3Client.putObject(new PutObjectRequest(getAWSBucketName(), fileId, inputStream, null));
        } else {
            throw new SdkClientException(FOLDER_ERROR_MESSAGE);
        }
    }

    /**
     * Download a file from S3
     *
     * @return String
     */
    @Override
    public String downloadFile(String fileId) {
        try {
            AmazonS3 s3Client = getAmazonS3Client();
            validateS3Details(s3Client);
            S3Object s3Object = getObjectInS3(getLocation(fileId), s3Client);
            byte[] byteArray = IOUtils.toByteArray(s3Object.getObjectContent());
            return new String(byteArray);
        } catch (Exception e) {
            LOG.error(e, new HashMap<>() {{
                put("error", "Unable to fetch ixbrl from S3");
            }});
            return null;
        }
    }

    /**
     * Get file meta data
     *
     * @return String
     */
    @Override
    public S3Object getFileObject(String fileId) {
        try {
            AmazonS3 s3Client = getAmazonS3Client();
            validateS3Details(s3Client);

            return s3Client.getObject(new GetObjectRequest(getAWSBucketName(), fileId));
        } catch (Exception e) {
            LOG.error(e, new HashMap<>() {{
                put("error", "Unable to fetch ixbrl from S3");
            }});
            return null;
        }
    }

    /**
     * Get file meta tags
     *
     * @return Object containing Map of Tags
     */
    @Override
    public GetObjectTaggingResult getFileTags(String fileId) {
        try {
            AmazonS3 s3Client = getAmazonS3Client();
            validateS3Details(s3Client);
            return s3Client.getObjectTagging(new GetObjectTaggingRequest(getAWSBucketName(), fileId));
        } catch (Exception e) {
            LOG.error(e, new HashMap<>() {{
                put("error", "Unable to fetch ixbrl from S3");
            }});
            return null;
        }
    }

    /**
     * Delete an object in S3
     *
     * @throws SdkClientException
     */
    @Override
    public void deleteFile(String fileId) {
        AmazonS3 s3Client = getAmazonS3Client();
        validateS3Details(s3Client);

        if (validatePathExists(s3Client)) {
            s3Client.deleteObject(new DeleteObjectRequest(getAWSBucketName(), fileId));
        } else {
            throw new SdkClientException(FOLDER_ERROR_MESSAGE);
        }

    }

    /**
     * Get an object in S3
     *
     * @throws SdkClientException
     */
    protected S3Object getObjectInS3(String location, AmazonS3 s3Client) {

        String bucket = getBucketFromS3Location(location);
        String fileId = getKeyFromS3Location(location);

        if (!s3Client.doesObjectExist(bucket, fileId))
            throw new SdkClientException("S3 path does not exist: " + location);

        return s3Client.getObject(new GetObjectRequest(bucket, fileId));
    }


    protected String getBucketFromS3Location(String location) {
        String path = location.substring(SPLIT_S3_PATH_SUBSTRING);
        return path.split("/")[0];
    }

    /**
     * Get the fileId from the S3 location
     */
    protected String getKeyFromS3Location(String location) {
        String path = location.substring(SPLIT_S3_PATH_SUBSTRING);
        String[] pathSegments = path.split("/");

        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 1; i < pathSegments.length - 1; i++) {
            stringBuffer.append(pathSegments[i]);
            stringBuffer.append('/');
        }
        stringBuffer.append(pathSegments[pathSegments.length - 1]);

        return stringBuffer.toString();
    }

    /**
     * Get the bucket name on S3
     */
    private String getAWSBucketName() {
        return getSplitS3path()[0];
    }

    private String getPathIfExists() {
        if (getSplitS3path().length > 1) {
            return getSplitS3path()[1] + SUFFIX;
        } else {
            return "";
        }
    }

    /**
     * Get the S3 path split
     */
    private String[] getSplitS3path() {
        String s3Path = configuration.getS3Path();
        String path = s3Path.substring(SPLIT_S3_PATH_SUBSTRING);
        return path.split("/", GET_S3_PATH_FROM_SPLIT);
    }

    /**
     * Configure the S3 client
     *
     * @param credentials
     * @return An {@link AmazonS3}
     */
    private AmazonS3 getAmazonS3(AWSCredentials credentials) {
        ClientConfiguration clientConfiguration = getClientConfiguration(getProxyHost(),
                getProxyPort(),
                configuration.getProtocol());
        return new AmazonS3Client(credentials, clientConfiguration);
    }

    /**
     * Get the proxy host if it has been defined
     *
     * @return A {@link String} or null
     */
    private String getProxyHost() {
        return System.getenv(PROXY_HOST_PROPERTY);
    }

    /**
     * Get the proxy port if it has been defined
     *
     * @return An {@link Integer} or null
     */
    private Integer getProxyPort() {
        String proxyPortString = System.getenv(PROXY_PORT_PROPERTY);
        return proxyPortString == null ? null : Integer.valueOf(proxyPortString);
    }

    /**
     * Get the AWS credentials
     */
    protected AmazonS3 getAmazonS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(configuration.getAccessKeyId(),
                configuration.getSecretAccessKey());
        return getAmazonS3Client(credentials);
    }

    /**
     * Configure the S3 client
     *
     * @param credentials
     * @return An {@link AmazonS3}
     */
    private AmazonS3 getAmazonS3Client(AWSCredentials credentials) {
        ClientConfiguration clientConfiguration = getClientConfiguration(getProxyHost(),
                getProxyPort(),
                configuration.getProtocol());
        return new AmazonS3Client(credentials, clientConfiguration);
    }

    /**
     * Configure the Proxy Host Proxy port and the Protocol
     *
     * @param httpProxyHostName
     * @param httpProxyPort
     * @param protocol
     * @return A {@link ClientConfiguration}
     */
    protected ClientConfiguration getClientConfiguration(String httpProxyHostName, Integer httpProxyPort, String protocol) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();

        if (httpProxyHostName != null && !httpProxyHostName.trim().isEmpty()) {
            clientConfiguration.setProxyHost(httpProxyHostName);
        }

        if (httpProxyPort != null) {
            clientConfiguration.setProxyPort(httpProxyPort);
        }

        clientConfiguration.setProtocol("http".equalsIgnoreCase(protocol) ? Protocol.HTTP : Protocol.HTTPS);

        return clientConfiguration;
    }

    private String getLocation(String fileId) {
        return String.format("%s/%s", configuration.getS3Path(), fileId);
    }

    private boolean validateS3Path() {
        return configuration.getS3Path().trim().toLowerCase().startsWith(S3_PATH_PREFIX);
    }

    private boolean validateBucketName() {
        return !getAWSBucketName().isEmpty();
    }

    private void validateS3Details(AmazonS3 s3Client) {
        if (!validateS3Path())
            throw new SdkClientException("S3 path is invalid");
        if (!validateBucketName())
            throw new SdkClientException("bucket name is invalid");
        if (!s3Client.doesBucketExist(getAWSBucketName()))
            throw new SdkClientException("bucket does not exist");
    }

    public boolean validatePathExists(AmazonS3 s3Client) {
        return getPathIfExists().isEmpty() || s3Client.doesObjectExist(getAWSBucketName(), getPathIfExists().trim());
    }
}
