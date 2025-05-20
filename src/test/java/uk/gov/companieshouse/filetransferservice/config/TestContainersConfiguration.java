package uk.gov.companieshouse.filetransferservice.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration
public class  TestContainersConfiguration {

    private final Environment env;

    public TestContainersConfiguration(Environment env) {
        this.env = env;
    }

    @Primary
    @Bean("localstack.s3.client")
    public S3Client s3Client() throws URISyntaxException {
        return S3Client.builder()
                .credentialsProvider(getCredentialsProvider())
                .endpointOverride(new URI(Optional.ofNullable(env.getProperty("spring.cloud.aws.s3.endpoint"))
                        .orElseThrow(() ->new IllegalArgumentException("Missing S3 endpoint"))))
                .build();
    }

    private StaticCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                env.getProperty("aws.accessKeyId"),
                env.getProperty("aws.secretAccessKey")));
    }
}
