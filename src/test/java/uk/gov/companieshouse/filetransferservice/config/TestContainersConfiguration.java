package uk.gov.companieshouse.filetransferservice.config;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    @Autowired
    private Environment env;

    @Bean(name = "integrationTestClient")
    public S3Client s3Client() throws URISyntaxException {
        return S3Client.builder()
                .credentialsProvider(getCredentialsProvider())
                .endpointOverride(new URI(env.getProperty("spring.cloud.aws.s3.endpoint")))
                .build();
    }

    private StaticCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                env.getProperty("spring.cloud.aws.credentials.access-key"),
                env.getProperty("spring.cloud.aws.credentials.secret-key")));
    }
}
