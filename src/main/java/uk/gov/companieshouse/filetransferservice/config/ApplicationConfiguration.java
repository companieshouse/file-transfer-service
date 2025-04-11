package uk.gov.companieshouse.filetransferservice.config;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


@Configuration
public class ApplicationConfiguration {

    @Value("${application.namespace}")
    private String applicationNameSpace;

    @Value("${aws.region:eu-west-2}")
    private String region;

    @Value("${aws.endpoint:empty}")
    private String s3endpoint;

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
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

    /**
     * Creates the user interceptor used by the application.
     *
     * @return user interceptor
     */
    @Bean
    public InternalUserInterceptor userInterceptor() {
        return new InternalUserInterceptor(applicationNameSpace);
    }

    @Bean
    @Profile("!local")
    public S3Client s3Client(){
        S3ClientBuilder builder =  S3Client.builder()
            .credentialsProvider(DefaultCredentialsProvider.create());
        return builder.build();
    }

    @Bean
    @Profile("local")
    public S3Client s3ClientLocal(){
        S3ClientBuilder builder =  S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
            builder.serviceConfiguration(t -> t.pathStyleAccessEnabled(true));
            builder.endpointOverride(URI.create(s3endpoint));
        return builder.build();
    }
}
