package uk.gov.companieshouse.filetransferservice.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
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
    public Logger getLogger() {
        return LoggerFactory.getLogger(applicationNameSpace);
    }

    @Bean
    public InternalUserInterceptor getUserInterceptor() {
        return new InternalUserInterceptor(applicationNameSpace);
    }

    @Bean
    public ClientConfiguration getClientConfiguration() {
        return new ClientConfiguration();
    }

    @Bean
    public AmazonS3ClientBuilder getAmazonS3ClientBuilder() {
        return AmazonS3ClientBuilder.standard();
    }

    @Bean
    public AmazonS3 getAmazonS3Client(AWSServiceProperties properties, ClientConfiguration clientConfiguration, AmazonS3ClientBuilder amazonS3ClientBuilder) {
        String httpProxyHostName = properties.getProxyHost();
        if (!isNullOrEmpty(httpProxyHostName)) {
            clientConfiguration.setProxyHost(httpProxyHostName);
        }

        Integer httpProxyPort = properties.getProxyPort();
        if (httpProxyPort != null) {
            clientConfiguration.setProxyPort(httpProxyPort);
        }

        AWSCredentials credentials = new BasicAWSCredentials(properties.getAccessKeyId(), properties.getSecretAccessKey());

        return amazonS3ClientBuilder
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(clientConfiguration)
                .withRegion(properties.getRegion())
                .build();
    }
}

