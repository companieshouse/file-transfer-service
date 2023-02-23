package uk.gov.companieshouse.filetransferservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;
import uk.gov.companieshouse.filetransferservice.service.impl.AmazonFileTransferImpl;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


@Configuration
public class ApplicationConfiguration {
    @Value("${application.namespace}")
    private String applicationNameSpace;

    /**
     * Creates the Amazon File Transfer object used by the application.
     *
     * @return the transfer object
     */
    @Bean
    public AmazonFileTransfer amazonFileTransfer(AWSServiceProperties awsServiceProperties) {
        return new AmazonFileTransferImpl(awsServiceProperties);
    }

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

}

