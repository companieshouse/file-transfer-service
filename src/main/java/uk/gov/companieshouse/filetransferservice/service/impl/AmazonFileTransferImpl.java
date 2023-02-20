package uk.gov.companieshouse.filetransferservice.service.impl;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.io.InputStream;

@Component
public class AmazonFileTransferImpl implements AmazonFileTransfer {

    private static final String SUFFIX = "/";
    private static final String S3_PATH_PREFIX = "s3://";
    private static final String FOLDER_ERROR_MESSAGE = "Subfolder path does not exist";
    private static final String PROXY_HOST_PROPERTY = "IMAGE_CLOUD_PROXY_HOST";
    private static final String PROXY_PORT_PROPERTY = "IMAGE_CLOUD_PROXY_PORT";

    private static final int SPLIT_S3_PATH_SUBSTRING = 5;
    private static final int GET_S3_PATH_FROM_SPLIT = 2;

    private AWSServiceProperties configuration;

    @Autowired
    public AmazonFileTransferImpl(AWSServiceProperties configuration) {
        this.configuration = configuration;
    }

    /**
     * Upload the file in S3
     */
    @Override
    public void uploadFileInS3(String fileName, InputStream inputStream, ObjectMetadata omd) {
        AmazonS3 s3client = getAmazonS3Client();
        validateS3Details(s3client);
        if (validatePathExists(s3client)) {
            s3client.putObject(new PutObjectRequest(getAWSBucketName(), getKey(fileName), inputStream, omd));
        } else {
            throw new SdkClientException(FOLDER_ERROR_MESSAGE);
        }
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

    private String getKey(String fileName) {
        String key;
        if (getSplitS3path().length > 1) {
            key = getPathIfExists() + fileName;
        } else {
            key = fileName;
        }
        return key;
    }

    private boolean validateS3Path() {
        return configuration.getS3Path().trim().toLowerCase().startsWith(S3_PATH_PREFIX);
    }

    private boolean validateBucketName() {
        return !getAWSBucketName().isEmpty();
    }

    private void validateS3Details(AmazonS3 s3client) {
        if (!validateS3Path())
            throw new SdkClientException("S3 path is invalid");
        if (!validateBucketName())
            throw new SdkClientException("bucket name is invalid");
        if (!s3client.doesBucketExist(getAWSBucketName()))
            throw new SdkClientException("bucket does not exist");
    }

    private boolean validatePathExists(AmazonS3 s3client) {
        return getPathIfExists().isEmpty() || s3client.doesObjectExist(getAWSBucketName(), getPathIfExists().trim());
    }
}
