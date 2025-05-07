package uk.gov.companieshouse.filetransferservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
class AwsSdkV2Config {

    private final String awsRegion;
    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;

    AwsSdkV2Config(@Value("${aws.region}") String awsRegion,
            @Value("${aws.accessKeyId}") String awsAccessKeyId,
            @Value("${aws.secretAccessKey}") String awsSecretAccessKey) {
        this.awsRegion = awsRegion;
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
    }

    @Bean
    S3Client s3Client() {
        AwsCredentialsProvider credentialsProvider = getCredentialsProvider();

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private AwsCredentialsProvider getCredentialsProvider() {
        AwsCredentialsProvider awsCredentialsProvider = EnvironmentVariableCredentialsProvider.create();
        try {
            AwsCredentials awsCredentials = awsCredentialsProvider.resolveCredentials();
            return StaticCredentialsProvider.create(awsCredentials);

        } catch(Exception ex) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey));
        }
    }
}
