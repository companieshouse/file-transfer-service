package uk.gov.companieshouse.filetransferservice.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;

@Configuration
public class ApplicationConfiguration {

    @Value("${application.namespace}")
    private String applicationNameSpace;

    /**
     * Creates the logger used by the application.
     *
     * @return the logger
     */
    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger(applicationNameSpace);
    }

    @Bean
    public AWSCredentials awsCredentials(@Value("${aws.accessKeyId}") String awsAccessKeyId,
                                         @Value("${aws.secretAccessKey}") String awsSecretAccessKey) {
        return new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
    }

    /**
     * Creates the user interceptor used by the application.
     *
     * @return user interceptor
     */
    @Bean
    public InternalUserInterceptor userInterceptor() {
        return new InternalUserInterceptor(applicationNameSpace);
    }

    /**
     * Creates an Amazon S3 client using the DefaultAWSCredentialsProviderChain. This client
     * configuration will include any proxy settings if they are provided. Proxy settings
     * and region information are extracted from the supplied AWSServiceProperties.
     * <p>
     * The DefaultAWSCredentialsProviderChain searches for credentials in the following order:
     * - Environment Variables (AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY)
     * - Java System Properties (aws.accessKeyId and aws.secretKey)
     * - Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs
     * - Credentials delivered through the Amazon EC2 container service if AWS_CONTAINER_CREDENTIALS_RELATIVE_URI
     *   environment variable is set and the security manager has permission to access the variable
     * - Instance profile credentials delivered through the Amazon EC2 metadata service
     * - Credentials from the AWS SSO login flow after being exported into the environment using the AWS CLI.
     *   Example: Use `eval "$(aws configure export-credentials --profile dev --format env)"` to set the required
     *   environment variables after an SSO login.
     * </p>
     * @param credentials         The AWSCredentials object containing the accessKey and secretKey.
     * @param properties          The AWSServiceProperties object containing the region, proxy host, and proxy port.
     *
     * @return An initialized AmazonS3 client that is ready to be used to communicate with the S3 service.
     */
    @Bean
    public AmazonS3 getAmazonS3Client(AWSCredentials credentials, AWSServiceProperties properties) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();

        String httpProxyHostName = properties.getProxyHost();
        if (!isNullOrEmpty(httpProxyHostName)) {
            clientConfiguration.setProxyHost(httpProxyHostName);
        }

        Integer httpProxyPort = properties.getProxyPort();
        if (httpProxyPort != null) {
            clientConfiguration.setProxyPort(httpProxyPort);
        }

        return AmazonS3Client.builder()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(properties.getRegion())
                .withClientConfiguration(clientConfiguration)
                .build();
    }
}

