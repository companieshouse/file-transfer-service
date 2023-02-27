package uk.gov.companieshouse.filetransferservice.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
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
    public InternalUserInterceptor userInterceptor() {
        return new InternalUserInterceptor(applicationNameSpace);
    }

    @Bean
    protected AmazonS3 getAmazonS3Client(AWSServiceProperties properties) {
        AWSCredentials credentials = new BasicAWSCredentials(properties.getAccessKeyId(), properties.getSecretAccessKey());

        ClientConfiguration clientConfiguration = new ClientConfiguration();

        String httpProxyHostName = properties.getProxyHost();
        Integer httpProxyPort = properties.getProxyPort();

        if (!isNullOrEmpty(httpProxyHostName)) {
            clientConfiguration.setProxyHost(httpProxyHostName);
        }

        if (httpProxyPort != null) {
            clientConfiguration.setProxyPort(httpProxyPort);
        }

        return new AmazonS3Client(credentials, clientConfiguration);
    }
}

